package com.moebius.entropy.service.order.boboo;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.DummyOrderConfig;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.dto.order.DividedDummyOrderDto;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.tradewindow.TradeWindowInflationVolumeResolver;
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService;
import com.moebius.entropy.util.EntropyRandomUtils;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class BobooDividedDummyOrderService {
	private static final String DISPOSABLE_ID_POSTFIX = "DIVIDED-DUMMY-ORDER";
	private final BobooOrderService orderService;
	private final TradeWindowQueryService tradeWindowQueryService;
	private final TradeWindowInflationVolumeResolver volumeResolver;
	private final DisposableOrderRepository disposableOrderRepository;
	private final EntropyRandomUtils randomUtils;

	public Mono<ServerResponse> executeDividedDummyOrders(DividedDummyOrderDto dividedDummyOrderDto) {
		if (dividedDummyOrderDto == null) {
			return Mono.empty();
		}

		InflationConfig inflationConfig = dividedDummyOrderDto.getInflationConfig();
		Market market = dividedDummyOrderDto.getMarket();

		if (inflationConfig == null || market == null) {
			return Mono.empty();
		}

		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
		BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

		Flux.fromStream(IntStream.rangeClosed(1, inflationConfig.getAskCount())
			.mapToObj(BigDecimal::valueOf)
			.map(multiplier -> marketPrice.add(priceUnit.multiply(multiplier))))
			.subscribeOn(Schedulers.parallel())
			.map(price -> executeDummyOrder(dividedDummyOrderDto, OrderPosition.ASK, price))
			.subscribe(disposable -> disposableOrderRepository.set(market.getExchange() + "-" + market.getSymbol() + "-" + DISPOSABLE_ID_POSTFIX,
				disposable));

		Flux.fromStream(IntStream.rangeClosed(1, inflationConfig.getBidCount())
			.mapToObj(BigDecimal::valueOf)
			.map(multiplier -> marketPrice.subtract(priceUnit.multiply(multiplier))))
			.subscribeOn(Schedulers.parallel())
			.map(price -> executeDummyOrder(dividedDummyOrderDto, OrderPosition.BID, price))
			.subscribe(disposable -> disposableOrderRepository.set(market.getExchange() + "-" + market.getSymbol() + "-" + DISPOSABLE_ID_POSTFIX,
				disposable));

		return ServerResponse.ok().build();
	}

	public Mono<ServerResponse> cancelDividedDummyOrders(Market market, String disposableId) {
		Optional.ofNullable(disposableOrderRepository.get(market.getExchange() + "-" + market.getSymbol() + "-" + disposableId))
			.ifPresent(disposables -> disposables.forEach(Disposable::dispose));

		return ServerResponse.ok().build();
	}

	private Disposable executeDummyOrder(DividedDummyOrderDto dividedDummyOrderDto, OrderPosition orderPosition, BigDecimal price) {
		return Mono.fromCallable(() -> {
			Market market = dividedDummyOrderDto.getMarket();

			List<OrderRequest> orderRequests = new ArrayList<>();
			int orderCount = 0;
			Duration orderDuration = Duration.ZERO;

			while (true) {
				if (orderPosition == OrderPosition.ASK) {
					DummyOrderConfig askOrderConfig = dividedDummyOrderDto.getAskOrderConfig();

					orderRequests = volumeResolver.getDividedVolume(market, orderPosition, askOrderConfig.getOrderRange()).stream()
						.map(volume -> new OrderRequest(market, orderPosition, price, volume))
						.collect(Collectors.toList());
					orderCount = randomUtils.getRandomInteger(askOrderConfig.getOrderCountRange().getLeft(),
						askOrderConfig.getOrderCountRange().getRight());
					orderDuration = Duration.ofMillis((long) askOrderConfig.getSecondPeriod() / orderCount * 1000);
				} else if (orderPosition == OrderPosition.BID) {
					DummyOrderConfig bidOrderConfig = dividedDummyOrderDto.getBidOrderConfig();

					orderRequests = volumeResolver.getDividedVolume(market, orderPosition, bidOrderConfig.getOrderRange()).stream()
						.map(volume -> new OrderRequest(market, orderPosition, price, volume))
						.collect(Collectors.toList());
					orderCount = randomUtils.getRandomInteger(bidOrderConfig.getOrderCountRange().getLeft(),
						bidOrderConfig.getOrderCountRange().getRight());
					orderDuration = Duration.ofMillis((long) bidOrderConfig.getSecondPeriod() / orderCount * 1000);
				}

				for (int i = 0; i < orderCount; ++i) {
					for (OrderRequest orderRequest : orderRequests) {
						orderService.requestManualOrder(orderRequest)
							.delayElement(orderDuration)
							.subscribe(orderService::cancelOrder);
					}
				}
			}
		}).subscribe();
	}
}
