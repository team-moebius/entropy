package com.moebius.entropy.service.exchange;

import com.moebius.entropy.domain.order.ApiKey;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ExchangeService<CANCEL_REQ, CANCEL_RES, ORDER_REQ, ORDER_RES, ORDERS> {
	Flux<ORDERS> getOpenOrders(String symbol, ApiKey apiKey);
	Mono<CANCEL_RES> cancelOrder(CANCEL_REQ cancelRequest, ApiKey apiKey);
	Mono<ORDER_RES> requestOrder(ORDER_REQ orderRequest, ApiKey apiKey);
	void inflateOrdersByOrderBook(String symbol);
}
