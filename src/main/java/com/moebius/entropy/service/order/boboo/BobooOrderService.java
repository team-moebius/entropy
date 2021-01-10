package com.moebius.entropy.service.order.boboo;

import com.moebius.entropy.assembler.BobooOrderExchangeAssembler;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.dto.exchange.order.ApiKeyDto;
import com.moebius.entropy.service.exchange.boboo.BobooExchangeService;
import com.moebius.entropy.service.order.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class BobooOrderService implements OrderService {
    private final BobooExchangeService exchangeService;
    private final BobooOrderExchangeAssembler assembler;
    private final Map<String, List<Order>> orderListForSymbol;
    private final Set<String> automaticOrderIds;
    private final ApiKeyDto apiKeyDto;

    public BobooOrderService(BobooExchangeService exchangeService,
                             BobooOrderExchangeAssembler assembler,
                             @Value("exchange.boboo.apikey.accessKey") String accessKey,
                             @Value("exchange.boboo.apikey.eecret") String secretKey) {
        this.exchangeService = exchangeService;
        this.assembler = assembler;
        orderListForSymbol = new HashMap<>();
        automaticOrderIds = new LinkedHashSet<>();
        apiKeyDto = ApiKeyDto.builder().accessKey(accessKey).secretKey(secretKey).build();
    }

    public Flux<Order> fetchAutomaticOrdersFor(Market market){
        return fetchAllOrdersFor(market)
                .filter(order -> automaticOrderIds.contains(order.getOrderId()));
    }

    public Flux<Order> fetchManualOrdersFor(Market market){
        return fetchAllOrdersFor(market)
                .filter(order -> !automaticOrderIds.contains(order.getOrderId()));
    }

    public Flux<Order> fetchAllOrdersFor(Market market){
        return Flux.fromIterable(getAllOrdersForMarket(market.getSymbol()));
    }

    private List<Order> getAllOrdersForMarket(String symbol) {
        return orderListForSymbol.getOrDefault(symbol, Collections.emptyList());
    }

    public Mono<Order> requestOrder(OrderRequest orderRequest){
        return requestOrderWith(orderRequest, this::trackOrderAsAutomaticOrder);
    }

    public Mono<Order> requestManualOrder(OrderRequest orderRequest){
        return requestOrderWith(orderRequest, this::trackOrder);
    }

    public Mono<Order> cancelOrder(Order order){
        return Optional.ofNullable(order)
                .filter(requestedOrder-> getAllOrdersForMarket(order.getMarket().getSymbol())
                        .stream()
                        .anyMatch(trackedOrder->trackedOrder.getOrderId().equals(order.getOrderId()))
                )
                .map(assembler::convertToCancelRequest)
                .map(cancelRequest -> exchangeService.cancelOrder(cancelRequest, apiKeyDto))
                .map(cancelMono->cancelMono
                        .map(bobooCancelResponse -> order)
                        .doOnSuccess(this::releaseOrderFromTracking)
                )
                .orElse(Mono.empty());

    }

    public Mono<Integer> updateOrders(List<Order> orders) {
        return Mono.just(orders)
                .map(updatedOrders->{
                    Set<String> aliveOrderIds = new LinkedHashSet<>();
                    orderListForSymbol.clear();
                    updatedOrders.forEach(order -> {
                        aliveOrderIds.add(order.getOrderId());
                        trackOrder(order);
                    });

                    List<String> aliveAutomaticOrderIds = automaticOrderIds.stream()
                            .filter(aliveOrderIds::contains)
                            .collect(Collectors.toList());

                    automaticOrderIds.clear();
                    automaticOrderIds.addAll(aliveAutomaticOrderIds);
                    return automaticOrderIds;
                })
                .map(Set::size);
    }

    private Mono<Order> requestOrderWith(OrderRequest orderRequest, Consumer<Order> afterOrderCompleted){
        return Optional.ofNullable(orderRequest)
                .map(assembler::convertToOrderRequest)
                .map(bobooOrderRequest->exchangeService.requestOrder(bobooOrderRequest, apiKeyDto))
                .map(orderMono->orderMono.map(assembler::convertToOrder)
                        .doOnSuccess(afterOrderCompleted)
                )
                .orElse(Mono.empty());
    }

    private void trackOrderAsAutomaticOrder(Order order) {
        automaticOrderIds.add(order.getOrderId());
        trackOrder(order);
    }

    private void trackOrder(Order order) {
        List<Order> orders = orderListForSymbol.computeIfAbsent(
                order.getMarket().getSymbol(), symbol -> new LinkedList<>()
        );
        orders.add(order);
    }

    private void releaseOrderFromTracking(Order order){
        automaticOrderIds.remove(order.getOrderId());
        orderListForSymbol.computeIfPresent(order.getMarket().getSymbol(), (s, orders) -> {
            orders.removeIf(trackedOrder -> trackedOrder.getOrderId().equals(order.getOrderId()));
            return orders;
        });

    }
}
