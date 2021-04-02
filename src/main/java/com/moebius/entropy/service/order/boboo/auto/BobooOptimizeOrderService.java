package com.moebius.entropy.service.order.boboo.auto;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.service.exchange.boboo.BobooExchangeService;
import com.moebius.entropy.service.order.boboo.BobooOrderService;
import com.moebius.entropy.service.tradewindow.TradeWindowInflationVolumeResolver;
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class BobooOptimizeOrderService {
	private final static long DEFAULT_DELAY = 300L;
	private final BobooOrderService orderService;
	private final BobooExchangeService exchangeService;
	private final TradeWindowQueryService tradeWindowQueryService;
	private final TradeWindowInflationVolumeResolver volumeResolver;

	public Flux<Order> optimizeOrders(Market market) {
		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
		BigDecimal highestBidPrice = marketPrice.subtract(market.getTradeCurrency().getPriceUnit());

		exchangeService.inflateOrdersByOrderBook(market.getSymbol());

		return orderService.fetchOpenOrdersFor(market)
			.delayElements(Duration.ofMillis(DEFAULT_DELAY))
			.flatMap(orderService::cancelOrderWithoutTracking)
			.filter(order -> order.getPrice().compareTo(marketPrice) == 0 || order.getPrice().compareTo(highestBidPrice) == 0)
			.flatMap(order -> orderService.requestOrderWithoutTracking(new OrderRequest(market, order.getOrderPosition(), order.getPrice(),
				volumeResolver.getInflationVolume(market, order.getOrderPosition()))))
			.onErrorContinue((throwable, order) -> log.warn("[OptimizeOrder] Failed to cancel existent order. [{}]", order, throwable));
	}
}
