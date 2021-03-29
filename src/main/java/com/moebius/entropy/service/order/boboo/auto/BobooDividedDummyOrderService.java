package com.moebius.entropy.service.order.boboo.auto;

import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.DummyOrderRequest;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.order.config.DummyOrderConfig;
import com.moebius.entropy.dto.MarketDto;
import com.moebius.entropy.dto.order.DividedDummyOrderDto;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.order.boboo.BobooOrderService;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BobooDividedDummyOrderService {
	private final static String DISPOSABLE_ID_POSTFIX = "DIVIDED-DUMMY-ORDER";
	private final static long DEFAULT_DELAY = 1000L;

	private final BobooOrderService orderService;
	private final TradeWindowQueryService tradeWindowQueryService;
	private final TradeWindowInflationVolumeResolver volumeResolver;
	private final DisposableOrderRepository disposableOrderRepository;
	private final EntropyRandomUtils randomUtils;

	public Mono<ResponseEntity<?>> executeDividedDummyOrders(DividedDummyOrderDto dividedDummyOrderDto) {
		if (dividedDummyOrderDto == null) {
			return Mono.just(ResponseEntity.badRequest().build());
		}

		MarketDto marketDto = dividedDummyOrderDto.getMarket();
		InflationConfig inflationConfig = dividedDummyOrderDto.getInflationConfig();

		if (marketDto == null || inflationConfig == null) {
			return Mono.just(ResponseEntity.badRequest().build());
		}

		Disposable disposable = getDummyOrderRequestsPeriodicFlux(dividedDummyOrderDto)
			.subscribeOn(Schedulers.parallel())
			.subscribe(dummyOrderRequests -> dummyOrderRequests.forEach(dummyOrderRequest -> {
				try {
					Thread.sleep(DEFAULT_DELAY);
				} catch (InterruptedException e) {
					log.error("Failed when waiting for subscribing dummy orders.", e);
				}
				requestAndCancelDummyOrder(dummyOrderRequest).subscribe();
			}));

		String disposableId = marketDto.getExchange() + "-" + marketDto.getSymbol() + "-" + DISPOSABLE_ID_POSTFIX;
		disposableOrderRepository.set(disposableId, disposable);
		return Mono.just(ResponseEntity.ok(disposableId));
	}

	private Flux<List<DummyOrderRequest>> getDummyOrderRequestsPeriodicFlux(DividedDummyOrderDto dto) {
		Duration totalDuration = getTotalDuration(dto);

		return Flux.interval(Duration.ZERO, totalDuration)
			.map(tick -> getDummyOrderRequests(dto));
	}

	private Duration getTotalDuration(DividedDummyOrderDto dto) {
		InflationConfig inflationConfig = dto.getInflationConfig();
		DummyOrderConfig askOrderConfig = dto.getAskOrderConfig();
		DummyOrderConfig bidOrderConfig = dto.getBidOrderConfig();

		return Duration.ofMillis((long) (Math.max(askOrderConfig.getPeriod(), bidOrderConfig.getPeriod()) + (inflationConfig.getAskCount()
			+ inflationConfig.getBidCount())) * DEFAULT_DELAY);
	}

	private List<DummyOrderRequest> getDummyOrderRequests(DividedDummyOrderDto dto) {
		MarketDto marketDto = dto.getMarket();

		InflationConfig inflationConfig = dto.getInflationConfig();
		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(marketDto.toDomainEntity());
		BigDecimal priceUnit = marketDto.getTradeCurrency().getPriceUnit();

		return Stream.concat(IntStream.range(0, inflationConfig.getAskCount())
				.mapToObj(BigDecimal::valueOf)
				.map(multiplier -> marketPrice.add(priceUnit.multiply(multiplier)))
				.map(price -> getDummyOrderRequest(dto, OrderPosition.ASK, price)),
			IntStream.rangeClosed(1, inflationConfig.getBidCount())
				.mapToObj(BigDecimal::valueOf)
				.map(multiplier -> marketPrice.subtract(priceUnit.multiply(multiplier)))
				.map(price -> getDummyOrderRequest(dto, OrderPosition.BID, price)))
			.collect(Collectors.toList());
	}

	private DummyOrderRequest getDummyOrderRequest(DividedDummyOrderDto dto, OrderPosition orderPosition, BigDecimal price) {
		List<BigDecimal> dividedVolumes = volumeResolver.getDividedVolume(dto, orderPosition);
		DummyOrderConfig dummyOrderConfig = getDummyOrderConfig(dto, orderPosition);
		int reorderCount = getReorderCount(dummyOrderConfig);

		return DummyOrderRequest.builder()
			.orderRequests(dividedVolumes.stream()
				.map(dividedVolume -> OrderRequest.builder()
					.market(dto.getMarket().toDomainEntity())
					.orderPosition(orderPosition)
					.price(price)
					.volume(dividedVolume)
					.build())
				.collect(Collectors.toList()))
			.reorderCount(reorderCount)
			.delay(getDelay(dummyOrderConfig, reorderCount))
			.build();
	}

	private DummyOrderConfig getDummyOrderConfig(DividedDummyOrderDto dto, OrderPosition orderPosition) {
		if (orderPosition == OrderPosition.ASK) {
			return dto.getAskOrderConfig();
		} else {
			return dto.getBidOrderConfig();
		}
	}

	private int getReorderCount(DummyOrderConfig dummyOrderConfig) {
		return randomUtils.getRandomInteger(dummyOrderConfig.getMinReorderCount(), dummyOrderConfig.getMaxReorderCount());
	}

	private Duration getDelay(DummyOrderConfig dummyOrderConfig, int reorderCount) {
		if (reorderCount < 1) {
			reorderCount = 1;
		}

		return Duration.ofMillis((long) dummyOrderConfig.getPeriod() * 500 / reorderCount);
	}

	private Flux<Order> requestAndCancelDummyOrder(DummyOrderRequest dummyOrderRequest) {
		return Flux.range(0, dummyOrderRequest.getReorderCount())
			.flatMapIterable(count -> dummyOrderRequest.getOrderRequests())
			.flatMap(orderService::requestOrderWithoutTracking)
			.doOnNext(order -> log.info("[DummyOrder] Succeeded to request dummy order. [{}]", order))
			.doOnError(throwable -> log.warn("[DummyOrder] Failed to request dummy order. [{}]",
				((WebClientResponseException) throwable).getResponseBodyAsString()))
			.delayElements(dummyOrderRequest.getDelay())
			.flatMap(orderService::cancelOrderWithoutTracking)
			.delayElements(dummyOrderRequest.getDelay())
			.doOnNext(order -> log.info("[DummyOrder] Succeeded to cancel dummy order. [{}]", order))
			.doOnError(throwable -> log.warn("[DummyOrder] Failed to cancel dummy order. [{}]",
				((WebClientResponseException) throwable).getResponseBodyAsString()));
	}
}
