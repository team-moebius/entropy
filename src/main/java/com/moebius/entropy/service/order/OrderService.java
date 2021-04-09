package com.moebius.entropy.service.order;

import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import reactor.core.publisher.Mono;

public interface OrderService {

    Mono<Order> requestOrder(OrderRequest orderRequest);

    Mono<Order> requestManualOrder(OrderRequest orderRequest);

    Mono<Order> cancelOrder(Order order);
}
