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
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

		MarketDto market = dividedDummyOrderDto.getMarket();
		InflationConfig inflationConfig = dividedDummyOrderDto.getInflationConfig();

		if (market == null || inflationConfig == null) {
			return Mono.just(ResponseEntity.badRequest().build());
		}

		// TODO : duration should be refined more to catch the proper period up for randomly defined order.
		Disposable disposable = Flux.interval(Duration.ZERO, Duration.ofSeconds(70))
			.subscribeOn(Schedulers.parallel())
			.flatMap(tick -> dummyOrdersMono(dividedDummyOrderDto))
			.subscribe();

		String disposableId = market.getExchange() + "-" + market.getSymbol() + "-" + DISPOSABLE_ID_POSTFIX;
		disposableOrderRepository.set(disposableId, disposable);
		return Mono.just(ResponseEntity.ok(disposableId));
	}

	private Mono<Void> dummyOrdersMono(DividedDummyOrderDto dividedDummyOrderDto) {
		MarketDto market = dividedDummyOrderDto.getMarket();
		InflationConfig inflationConfig = dividedDummyOrderDto.getInflationConfig();

		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market.toDomainEntity());
		BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

		return Mono.when(Flux.fromStream(IntStream.range(0, inflationConfig.getAskCount())
				.mapToObj(BigDecimal::valueOf)
				.map(multiplier -> marketPrice.add(priceUnit.multiply(multiplier))))
				.delayElements(Duration.ofMillis(500))
				.flatMap(price -> dividedDummyOrdersMono(dividedDummyOrderDto, OrderPosition.ASK, price)),
			Flux.fromStream(IntStream.rangeClosed(1, inflationConfig.getBidCount())
				.mapToObj(BigDecimal::valueOf)
				.map(multiplier -> marketPrice.subtract(priceUnit.multiply(multiplier))))
				.delayElements(Duration.ofMillis(500))
				.flatMap(price -> dividedDummyOrdersMono(dividedDummyOrderDto, OrderPosition.BID, price))
		);
	}

	private Mono<List<OrderRequest>> dividedDummyOrdersMono(DividedDummyOrderDto dividedDummyOrderDto, OrderPosition orderPosition,
		BigDecimal price) {
		return Mono.fromCallable(() -> {
			MarketDto marketDto = dividedDummyOrderDto.getMarket();
			Market market = marketDto.toDomainEntity();

			List<OrderRequest> orderRequests = new ArrayList<>();
			int reorderCount = 0;
			Duration orderDuration = Duration.ZERO;

			if (orderPosition == OrderPosition.ASK) {
				DummyOrderConfig askOrderConfig = dividedDummyOrderDto.getAskOrderConfig();

				orderRequests = volumeResolver.getDividedVolume(dividedDummyOrderDto, OrderPosition.ASK).stream()
					.map(volume -> new OrderRequest(market, orderPosition, price, volume))
					.collect(Collectors.toList());
				reorderCount = randomUtils.getRandomInteger(askOrderConfig.getMinReorderCount(), askOrderConfig.getMaxReorderCount());
				orderDuration = Duration.ofMillis((long) (askOrderConfig.getPeriod() / reorderCount * 1000));
			} else if (orderPosition == OrderPosition.BID) {
				DummyOrderConfig bidOrderConfig = dividedDummyOrderDto.getBidOrderConfig();

				orderRequests = volumeResolver.getDividedVolume(dividedDummyOrderDto, OrderPosition.BID).stream()
					.map(volume -> new OrderRequest(market, orderPosition, price, volume))
					.collect(Collectors.toList());
				reorderCount = randomUtils.getRandomInteger(bidOrderConfig.getMinReorderCount(), bidOrderConfig.getMaxReorderCount());
				orderDuration = Duration.ofMillis((long) (bidOrderConfig.getPeriod() / reorderCount * 1000));
			}

			for (int i = 0; i < reorderCount; ++i) {
				for (OrderRequest orderRequest : orderRequests) {
					int finalReorderCount = i;
					Duration finalOrderDuration = orderDuration;

					orderService.requestOrderWithoutTracking(orderRequest)
						.doOnSuccess(order -> log.info(
							"[DummyOrder] Succeeded in requesting dummy order. [orderRequest: {} | reorderCount : {} | orderDuration : {}]",
							orderRequest,
							finalReorderCount,
							finalOrderDuration.toMillis()))
						.doOnError(throwable -> log.error("[DummyOrder] Failed to request dummy order. {}]",
							((WebClientResponseException) throwable).getResponseBodyAsString()))
						.delayElement(orderDuration)
						.flatMap(orderService::cancelOrderWithoutTracking)
						.doOnSuccess(order -> log.info(
							"[DummyOrder] Succeeded to cancel dummy order. [order: {} | reorderCount : {} | orderDuration : {}]",
							order,
							finalReorderCount,
							finalOrderDuration.toMillis()))
						.doOnError(throwable -> log.error("[DummyOrder] Failed to cancel dummy order. {}]",
							((WebClientResponseException) throwable).getResponseBodyAsString()))
						.subscribe();
				}
				Thread.sleep(Duration.ofMillis(500).toMillis());
			}

			return orderRequests;
		});
	}
}
