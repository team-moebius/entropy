package com.moebius.entropy.service.order;

import com.moebius.entropy.assembler.OrderBobooExchangeAssembler;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.Order;
import com.moebius.entropy.domain.OrderRequest;
import com.moebius.entropy.dto.exchange.order.ApiKeyDto;
import com.moebius.entropy.dto.exchange.order.boboo.BobooOrderRequestDto;
import com.moebius.entropy.service.exchange.BobooService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class BobooOrderService implements OrderService{
    private final BobooService exchangeService;
    private final OrderBobooExchangeAssembler assembler;
    private final Map<String, List<Order>> orderListForSymbol;
    private final Set<String> automaticOrderIds;
    private final ApiKeyDto apiKeyDto;

    public BobooOrderService(BobooService exchangeService,
                             OrderBobooExchangeAssembler assembler,
                             String accessKey, String secretKey) {
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
        return Flux.fromIterable(getAllOrderForMarket(market.getSymbol()));
    }

    private List<Order> getAllOrderForMarket(String symbol) {
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
                .filter(requestedOrder-> getAllOrderForMarket(order.getSymbol())
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

    public Mono<Void> updateOrders(List<Order> orders) {
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
                .then();
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
                order.getSymbol(), symbol -> new LinkedList<>()
        );
        orders.add(order);
    }

    private void releaseOrderFromTracking(Order order){
        automaticOrderIds.remove(order.getOrderId());
        orderListForSymbol.computeIfPresent(order.getSymbol(), (s, orders) -> {
            orders.removeIf(trackedOrder -> trackedOrder.getOrderId().equals(order.getOrderId()));
            return orders;
        });

    }
}
