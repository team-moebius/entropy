package com.moebius.entropy.service.exchange;

import com.moebius.entropy.dto.exchange.order.ApiKeyDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ExchangeService<CANCEL_REQ, CANCEL_RES, ORDER_REQ, ORDER_RES, ORDERS> {
	Flux<ORDERS> getOpenOrders(String symbol, ApiKeyDto apiKey);
	Mono<CANCEL_RES> cancelOrder(CANCEL_REQ cancelRequest, ApiKeyDto apiKey);
	Mono<ORDER_RES> requestOrder(ORDER_REQ orderRequest, ApiKeyDto apiKey);
	void getAndUpdateByOrderBook(String symbol);
}
