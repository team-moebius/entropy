package com.moebius.entropy.service.order;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.Order;
import com.moebius.entropy.domain.OrderRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

    public Flux<Order> fetchAutomaticOrdersFor(Market market){
        return Flux.empty();
    }

    public Mono<Order> requestOrder(OrderRequest orderRequest){
        return Mono.empty();
    }

    public Mono<Order> cancelOrder(Order order){
        return Mono.empty();
    }


}
