package com.moebius.entropy.service.order;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.Order;
import com.moebius.entropy.domain.OrderRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface OrderService {
    Flux<Order> fetchAutomaticOrdersFor(Market market);

    Flux<Order> fetchManualOrdersFor(Market market);

    Flux<Order> fetchAllOrdersFor(Market market);

    Mono<Order> requestOrder(OrderRequest orderRequest);

    Mono<Order> requestManualOrder(OrderRequest orderRequest);

    Mono<Order> cancelOrder(Order order);

    Mono<Void> updateOrders(List<Order> orders);
}
