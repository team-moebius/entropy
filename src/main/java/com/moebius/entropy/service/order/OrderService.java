package com.moebius.entropy.service.order;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {
    Flux<Order> fetchAllOrdersFor(Market market);

    Mono<Order> requestOrder(OrderRequest orderRequest);

    Mono<Order> requestManualOrder(OrderRequest orderRequest);

    Mono<Order> cancelOrder(Order order);

    Mono<ResponseEntity<?>> stopOrder(String disposableId);

    Exchange getExchange();
}
