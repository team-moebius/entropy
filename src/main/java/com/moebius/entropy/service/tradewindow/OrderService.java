package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.Order;
import com.moebius.entropy.domain.OrderRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class OrderService {

    public Flux<Order> fetchAutomaticOrdersFor(Market market) {
        return null;
    }

    public Mono<Order> requestOrder(OrderRequest orderRequest) {
        return null;
    }

    public Mono<Order> cancelOrder(Order order) {
        return null;
    }


}
