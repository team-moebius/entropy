package com.moebius.entropy.service.order.boboo;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.DummyOrderConfig;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.dto.MarketDto;
import com.moebius.entropy.dto.order.DividedDummyOrderDto;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.tradewindow.TradeWindowInflationVolumeResolver;
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService;
import com.moebius.entropy.util.EntropyRandomUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BobooDividedDummyOrderService {
	private static final String DISPOSABLE_ID_POSTFIX = "DIVIDED-DUMMY-ORDER";
	private final BobooOrderService orderService;
	private final TradeWindowQueryService tradeWindowQueryService;
	private final TradeWindowInflationVolumeResolver volumeResolver;
	private final DisposableOrderRepository disposableOrderRepository;
	private final EntropyRandomUtils randomUtils;

	public Mono<ResponseEntity<?>> executeDividedDummyOrders(DividedDummyOrderDto dividedDummyOrderDto) {
		if (dividedDummyOrderDto == null) {
			return Mono.just(ResponseEntity.badRequest().build());
		}

		InflationConfig inflationConfig = dividedDummyOrderDto.getInflationConfig();
		MarketDto market = dividedDummyOrderDto.getMarket();

		if (inflationConfig == null || market == null) {
			return Mono.just(ResponseEntity.badRequest().build());
		}

		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market.toDomainEntity());
		BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

		Flux.fromStream(IntStream.rangeClosed(1, inflationConfig.getAskCount())
			.mapToObj(BigDecimal::valueOf)
			.map(multiplier -> marketPrice.add(priceUnit.multiply(multiplier))))
			.subscribeOn(Schedulers.parallel())
			.flatMap(price -> executeDummyOrderMono(dividedDummyOrderDto, OrderPosition.ASK, price))
			.subscribe(disposable -> disposableOrderRepository.set(market.getExchange() + "-" + market.getSymbol() + "-" + DISPOSABLE_ID_POSTFIX,
				disposable));

		Flux.fromStream(IntStream.rangeClosed(1, inflationConfig.getBidCount())
			.mapToObj(BigDecimal::valueOf)
			.map(multiplier -> marketPrice.subtract(priceUnit.multiply(multiplier))))
			.subscribeOn(Schedulers.parallel())
			.flatMap(price -> executeDummyOrderMono(dividedDummyOrderDto, OrderPosition.BID, price))
			.subscribe(disposable -> disposableOrderRepository.set(market.getExchange() + "-" + market.getSymbol() + "-" + DISPOSABLE_ID_POSTFIX,
				disposable));

		return Mono.just(ResponseEntity.ok().build());
	}

	public Mono<ResponseEntity<?>> cancelDividedDummyOrders(Market market, String disposableId) {
		Optional.ofNullable(disposableOrderRepository.get(market.getExchange() + "-" + market.getSymbol() + "-" + disposableId))
			.ifPresent(disposables -> disposables.forEach(Disposable::dispose));

		return Mono.just(ResponseEntity.ok().build());
	}

	private Mono<Disposable> executeDummyOrderMono(DividedDummyOrderDto dividedDummyOrderDto, OrderPosition orderPosition, BigDecimal price) {
		return Mono.fromCallable(() -> {
			MarketDto marketDto = dividedDummyOrderDto.getMarket();
			Market market = marketDto.toDomainEntity();

			List<OrderRequest> orderRequests = new ArrayList<>();
			int reorderCount = 0;
			Duration orderDuration = Duration.ZERO;

			while (true) {
				if (orderPosition == OrderPosition.ASK) {
					DummyOrderConfig askOrderConfig = dividedDummyOrderDto.getAskOrderConfig();

					orderRequests = volumeResolver.getDividedVolume(market, orderPosition, askOrderConfig.getMinDividedOrderCount(),
						askOrderConfig.getMaxDividedOrderCount()).stream()
						.map(volume -> new OrderRequest(market, orderPosition, price, volume))
						.collect(Collectors.toList());
					reorderCount = randomUtils.getRandomInteger(askOrderConfig.getMinReorderCount(), askOrderConfig.getMaxReorderCount());
					orderDuration = Duration.ofMillis((long) askOrderConfig.getPeriod() / reorderCount * 1000);
				} else if (orderPosition == OrderPosition.BID) {
					DummyOrderConfig bidOrderConfig = dividedDummyOrderDto.getBidOrderConfig();
					int minDividedOrderCount = bidOrderConfig.getMinDividedOrderCount();
					int maxDividedOrderCount = bidOrderConfig.getMaxDividedOrderCount();

					orderRequests = volumeResolver.getDividedVolume(market, orderPosition, minDividedOrderCount, maxDividedOrderCount).stream()
						.map(volume -> new OrderRequest(market, orderPosition, price, volume))
						.collect(Collectors.toList());
					reorderCount = randomUtils.getRandomInteger(bidOrderConfig.getMinReorderCount(), bidOrderConfig.getMaxReorderCount());
					orderDuration = Duration.ofMillis((long) bidOrderConfig.getPeriod() / reorderCount * 1000);
				}

				log.info("[DummyOrder] Start dummy order. [orderRequests: {} | reorderCount : {} | orderDuration : {}]", orderRequests, reorderCount,
					orderDuration.toMillis());
				for (int i = 0; i < reorderCount; ++i) {
					for (OrderRequest orderRequest : orderRequests) {
						orderService.requestManualOrder(orderRequest)
							.delayElement(orderDuration)
							.subscribe(orderService::cancelOrder);
					}
				}
			}
		});
	}
}
