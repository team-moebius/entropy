package com.moebius.entropy.service.order.boboo.auto;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.service.order.boboo.BobooOrderService;
import com.moebius.entropy.service.tradewindow.TradeWindowVolumeResolver;
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class BobooOptimizeOrderService {
	private final static long DEFAULT_DELAY = 300L;
	private final BobooOrderService orderService;
	private final TradeWindowQueryService tradeWindowQueryService;
	private final TradeWindowVolumeResolver volumeResolver;

	public Flux<Order> optimizeOrders(Market market) {
		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
		BigDecimal highestBidPrice = marketPrice.subtract(market.getTradeCurrency().getPriceUnit());

		return orderService.fetchAllOrdersFor(market)
			.delayElements(Duration.ofMillis(DEFAULT_DELAY))
			.flatMap(orderService::cancelOrder)
			.doOnNext(order -> log.info("[OptimizeOrder] Succeeded to cancel existent order. [{}]", order))
			.filter(order -> order.getPrice().compareTo(marketPrice) == 0 || order.getPrice().compareTo(highestBidPrice) == 0)
			.flatMap(order -> orderService.requestOrder(new OrderRequest(market, order.getOrderPosition(), order.getPrice(),
				volumeResolver.getInflationVolume(market, order.getOrderPosition()))))
			.onErrorContinue((throwable, o) -> log.warn("[OptimizeOrder] Failed to optimize order. [{}]",
				((WebClientResponseException) throwable).getResponseBodyAsString(), throwable));
	}
}
