package com.moebius.entropy.service.order;

import com.moebius.entropy.domain.Exchange;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderServiceFactory {
	private final Map<Exchange, OrderService> orderServiceMap;

	public OrderServiceFactory(List<OrderService> orderServices) {
		this.orderServiceMap = orderServices.stream()
			.collect(Collectors.toMap(OrderService::getExchange, Function.identity()));
	}

	public OrderService getOrderService(Exchange exchange) {
		return orderServiceMap.getOrDefault(exchange, null);
	}
}
