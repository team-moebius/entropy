package com.moebius.entropy.service.order.boboo.auto;

import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.DummyOrderConfig;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
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
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BobooDividedDummyOrderService {
	private final static String DISPOSABLE_ID_POSTFIX = "DIVIDED-DUMMY-ORDER";
	private final static long DEFAULT_DELAY = 300;

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

		Map<Float, OrderPosition> priceToPositions = getPriceToPositions(inflationConfig, marketDto);
		List<Disposable> disposables = priceToPositions.entrySet().stream()
			.map(entry -> getDividedOrderRequestsFlux(dividedDummyOrderDto, entry.getKey(), entry.getValue()))
			.map(orderRequestFlux -> orderRequestFlux
				.subscribeOn(Schedulers.parallel())
				.flatMap(orderRequest -> requestDummyOrder(orderRequest, getDividedDelay(dividedDummyOrderDto, orderRequest.getOrderPosition())))
				.subscribe())
			.collect(Collectors.toList());

		String disposableId = marketDto.getExchange() + "-" + marketDto.getSymbol() + "-" + DISPOSABLE_ID_POSTFIX;
		disposableOrderRepository.setAll(disposableId, disposables);
		return Mono.just(ResponseEntity.ok(disposableId));
	}

	private Map<Float, OrderPosition> getPriceToPositions(InflationConfig inflationConfig, MarketDto dto) {
		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(dto.toDomainEntity());
		BigDecimal priceUnit = dto.getTradeCurrency().getPriceUnit();
		Map<Float, OrderPosition> priceToPositions = new HashMap<>();

		IntStream.range(0, inflationConfig.getAskCount())
			.mapToObj(BigDecimal::valueOf)
			.map(multiplier -> marketPrice.add(priceUnit.multiply(multiplier)))
			.forEach(price -> priceToPositions.put(price.floatValue(), OrderPosition.ASK));

		IntStream.rangeClosed(1, inflationConfig.getBidCount())
			.mapToObj(BigDecimal::valueOf)
			.map(multiplier -> marketPrice.subtract(priceUnit.multiply(multiplier)))
			.forEach(price -> priceToPositions.put(price.floatValue(), OrderPosition.BID));

		return priceToPositions;
	}

	private Flux<OrderRequest> getDividedOrderRequestsFlux(DividedDummyOrderDto dto, float price, OrderPosition orderPosition) {
		return Flux.generate((SynchronousSink<OrderRequest> sink) ->
			volumeResolver.getDividedVolume(dto, orderPosition)
				// FIXME : change this sink to single one from multiple one in repeat form
				.forEach(volume -> sink.next(new OrderRequest(dto.getMarket().toDomainEntity(), orderPosition, BigDecimal.valueOf(price), volume))))
			.delayElements(Duration.ofMillis(DEFAULT_DELAY));
	}

	private Mono<Order> requestDummyOrder(OrderRequest orderRequest, long delay) {
		return orderService.requestOrderWithoutTracking(orderRequest)
			.doOnError(throwable -> log.error("[DummyOrder] Failed to request dummy order. {}]",
				((WebClientResponseException) throwable).getResponseBodyAsString()))
			.delayElement(Duration.ofMillis(delay))
			.flatMap(orderService::cancelOrderWithoutTracking)
			.doOnError(throwable -> log.error("[DummyOrder] Failed to cancel dummy order. {}]",
				((WebClientResponseException) throwable).getResponseBodyAsString()));
	}

	private long getDividedDelay(DividedDummyOrderDto dto, OrderPosition orderPosition) {
		DummyOrderConfig dummyOrderConfig;

		if (orderPosition == OrderPosition.ASK) {
			dummyOrderConfig = dto.getAskOrderConfig();
		} else {
			dummyOrderConfig = dto.getBidOrderConfig();
		}

		int reorderCount = randomUtils.getRandomInteger(dummyOrderConfig.getMinReorderCount(), dummyOrderConfig.getMaxReorderCount());

		if (reorderCount < 1) {
			reorderCount = 1;
		}

		return (long) (dummyOrderConfig.getPeriod() / reorderCount * 1000);
	}
}
