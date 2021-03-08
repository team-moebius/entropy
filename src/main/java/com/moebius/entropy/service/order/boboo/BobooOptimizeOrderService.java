package com.moebius.entropy.service.order.boboo;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.service.tradewindow.TradeWindowInflationVolumeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class BobooOptimizeOrderService {
	private final BobooOrderService orderService;
	private final TradeWindowInflationVolumeResolver volumeResolver;

	public Flux<Order> optimizeOrders(Market market) {
		return orderService.fetchAllOrdersFor(market)
			.flatMap(orderService::cancelOrder)
			.flatMap(order -> orderService.requestOrder(new OrderRequest(market, order.getOrderPosition(), order.getPrice(),
				volumeResolver.getInflationVolume(market, order.getOrderPosition()))));
	}
}
