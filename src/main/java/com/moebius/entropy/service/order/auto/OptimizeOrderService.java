package com.moebius.entropy.service.order.auto;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.service.order.OrderService;
import com.moebius.entropy.service.order.OrderServiceFactory;
import com.moebius.entropy.service.tradewindow.TradeWindowVolumeResolver;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizeOrderService {

	private final static long DEFAULT_DELAY = 300L;
	private final OrderServiceFactory orderServiceFactory;
	private final TradeWindowVolumeResolver volumeResolver;

	public Flux<Order> optimizeOrders(Market market) {
		OrderService orderService = orderServiceFactory.getOrderService(market.getExchange());

		return orderService.fetchAllOrdersFor(market)
			.delayElements(Duration.ofMillis(DEFAULT_DELAY))
			.flatMap(orderService::cancelOrder)
			.doOnNext(order -> log
				.info("[OptimizeOrder] Succeeded to cancel existent order. [{}]", order))
			.flatMap(order -> orderService
				.requestOrder(new OrderRequest(market, order.getOrderPosition(), order.getPrice(),
					volumeResolver.getInflationVolume(market, order.getOrderPosition()))))
			.doOnNext(
				order -> log.info("[OptimizeOrder] Succeeded to request order again. [{}]", order))
			.onErrorContinue(
				(throwable, o) -> log.warn("[OptimizeOrder] Failed to optimize order. [{}]",
					((WebClientResponseException) throwable).getResponseBodyAsString(), throwable));
	}
}
