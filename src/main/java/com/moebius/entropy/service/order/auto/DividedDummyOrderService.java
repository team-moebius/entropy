package com.moebius.entropy.service.order.auto;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.DummyOrderRequest;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.order.config.DummyOrderConfig;
import com.moebius.entropy.dto.MarketDto;
import com.moebius.entropy.dto.order.DividedDummyOrderDto;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.order.OrderService;
import com.moebius.entropy.service.order.OrderServiceFactory;
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService;
import com.moebius.entropy.service.tradewindow.TradeWindowVolumeResolver;
import com.moebius.entropy.util.EntropyRandomUtils;
import com.moebius.entropy.util.SpreadWindowResolveRequest;
import com.moebius.entropy.util.SpreadWindowResolver;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class DividedDummyOrderService {
	private final static String DISPOSABLE_ID_POSTFIX = "DIVIDED-DUMMY-ORDER";
	private final static long DEFAULT_DELAY = 300L;

	private final OrderServiceFactory orderServiceFactory;
	private final TradeWindowQueryService tradeWindowQueryService;
	private final TradeWindowVolumeResolver volumeResolver;
	private final DisposableOrderRepository disposableOrderRepository;
	private final EntropyRandomUtils randomUtils;
	private final SpreadWindowResolver spreadWindowResolver;

	public Mono<ResponseEntity<?>> executeDividedDummyOrders(DividedDummyOrderDto dividedDummyOrderDto) {
		if (dividedDummyOrderDto == null) {
			return Mono.just(ResponseEntity.badRequest().build());
		}

		MarketDto marketDto = dividedDummyOrderDto.getMarket();
		InflationConfig inflationConfig = dividedDummyOrderDto.getInflationConfig();

		if (marketDto == null || inflationConfig == null) {
			return Mono.just(ResponseEntity.badRequest().build());
		}

		Disposable disposable = getDummyOrderRequestsRepeatFlux(dividedDummyOrderDto)
			.subscribeOn(Schedulers.parallel())
			.flatMap(this::requestAndCancelDummyOrder)
			.subscribe();

		String disposableId = marketDto.getExchange() + "-" + marketDto.getSymbol() + "-" + DISPOSABLE_ID_POSTFIX;
		disposableOrderRepository.set(disposableId, disposable);
		return Mono.just(ResponseEntity.ok(disposableId));
	}

	private Flux<DummyOrderRequest> getDummyOrderRequestsRepeatFlux(DividedDummyOrderDto dto) {
		return Flux.defer(() -> Flux.fromIterable(getDummyOrderRequests(dto))
			.delayElements(Duration.ofMillis(DEFAULT_DELAY)))
			.repeat();
	}

	private List<DummyOrderRequest> getDummyOrderRequests(DividedDummyOrderDto dto) {
		MarketDto marketDto = dto.getMarket();

		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(marketDto.toDomainEntity());

		List<DummyOrderRequest> dummyOrderRequests = Stream.concat(
			resolveOrderRequestStream(marketPrice, BigDecimal::add, dto, OrderPosition.ASK),
			resolveOrderRequestStream(marketPrice, BigDecimal::subtract, dto, OrderPosition.BID)
		).collect(Collectors.toList());

		Collections.shuffle(dummyOrderRequests);

		return dummyOrderRequests;
	}
	private Stream<DummyOrderRequest> resolveOrderRequestStream(
		BigDecimal marketPrice, BinaryOperator<BigDecimal> operationOnPrice,
		DividedDummyOrderDto dto, OrderPosition position
	) {
		BigDecimal startPrice;
		int count, shiftCount;
		InflationConfig inflationConfig = dto.getInflationConfig();
		BigDecimal priceUnit = inflationConfig.getMarket().getTradeCurrency().getPriceUnit();

		if (OrderPosition.BID.equals(position)) {
			startPrice = operationOnPrice.apply(marketPrice, priceUnit);
			count = inflationConfig.getBidCount();
			shiftCount = inflationConfig.getBidShift();
		} else {
			startPrice = marketPrice;
			count = inflationConfig.getAskCount();
			shiftCount = inflationConfig.getAskShift();
		}

		SpreadWindowResolveRequest request = SpreadWindowResolveRequest.builder()
			.count(count)
			.startPrice(startPrice)
			.operationOnPrice(operationOnPrice)
			.spreadWindow(inflationConfig.getSpreadWindow())
			.priceUnit(priceUnit)
			.shiftCount(shiftCount)
			.build();

		return spreadWindowResolver.resolvePrices(request)
			.stream()
			.map(price->getDummyOrderRequest(dto, position, price));
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

		return Duration.ofMillis((long) dummyOrderConfig.getPeriod() * 1000 / reorderCount);
	}

	private Flux<Order> requestAndCancelDummyOrder(DummyOrderRequest dummyOrderRequest) {
		if (dummyOrderRequest == null) {
			log.error("[DummyOrder] null dummy order request");
			return Flux.empty();
		}

		Exchange exchange = Optional.ofNullable(dummyOrderRequest.getOrderRequests())
			.flatMap(orderRequests -> orderRequests.stream().findAny())
			.map(OrderRequest::getMarket)
			.map(Market::getExchange)
			.orElse(null);

		OrderService orderService = orderServiceFactory.getOrderService(exchange);

		if (orderService == null) {
			log.error("[DummyOrder] order service is not found by {}", exchange);
			return Flux.empty();
		}

		return Flux.range(0, dummyOrderRequest.getReorderCount())
			.flatMapIterable(count -> dummyOrderRequest.getOrderRequests())
			.delayElements(dummyOrderRequest.getDelay())
			.flatMap(orderService::requestOrder)
			.onErrorContinue((throwable, order) -> log.error("[DummyOrder] Failed to request dummy order. [{}]",
				((WebClientResponseException) throwable).getResponseBodyAsString()))
			.delayElements(Duration.ofMillis(DEFAULT_DELAY))
			.flatMap(orderService::cancelOrder)
			.onErrorContinue((throwable, order) -> log.error("[DummyOrder] Failed to cancel dummy order. [{}]",
				((WebClientResponseException) throwable).getResponseBodyAsString()))
			.doOnComplete(() -> log.info("[DummyOrder] Completed in creating & cancelling dummy orders. [{}]", dummyOrderRequest));
	}
}
