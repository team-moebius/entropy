package com.moebius.entropy.service.order.boboo;

import com.moebius.entropy.assembler.BobooOrderExchangeAssembler;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.exchange.boboo.BobooExchangeService;
import com.moebius.entropy.service.order.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Service
public class BobooOrderService implements OrderService {
    private final BobooExchangeService exchangeService;
    private final BobooOrderExchangeAssembler assembler;
    private final ApiKey apiKeyDto;
    private final DisposableOrderRepository disposableOrderRepository;

    public BobooOrderService(BobooExchangeService exchangeService,
                             BobooOrderExchangeAssembler assembler,
                             DisposableOrderRepository orderRepository,
                             @Value("${exchange.boboo.apikey.accessKey}") String accessKey,
                             @Value("${exchange.boboo.apikey.secretKey}") String secretKey) {
        this.exchangeService = exchangeService;
        this.assembler = assembler;
        this.disposableOrderRepository = orderRepository;
        apiKeyDto = new ApiKey();
        apiKeyDto.setAccessKey(accessKey);
        apiKeyDto.setSecretKey(secretKey);
    }

    @Override
    public Flux<Order> fetchAllOrdersFor(Market market) {
        return exchangeService.getOpenOrders(market.getSymbol(), apiKeyDto)
                .map(assembler::convertExchangeOrder);
    }

    @Override
    public Mono<Order> requestOrder(OrderRequest orderRequest) {
        return requestOrderWith(orderRequest, order -> {});
    }

    @Override
    public Mono<Order> requestManualOrder(OrderRequest orderRequest) {
        return requestOrderWith(orderRequest, order -> {});
    }

    @Override
    public Mono<Order> cancelOrder(Order order) {
        return Optional.ofNullable(order)
            .map(assembler::convertToCancelRequest)
            .map(cancelRequest -> exchangeService.cancelOrder(cancelRequest, apiKeyDto))
            .map(bobooCancelResponseMono -> bobooCancelResponseMono
                .map(bobooCancelResponse -> order)
                .doOnError(throwable -> log.error("[OrderCancel] Order cancellation failed for order id" + order.getOrderId(), throwable)))
            .orElse(Mono.empty());
    }

    public Mono<ResponseEntity<?>> stopOrder(String disposableId) {
        Optional.ofNullable(disposableOrderRepository.get(disposableId))
            .ifPresent(disposables -> disposables.forEach(Disposable::dispose));

        log.info("Succeeded in stopping order service. [{}]", disposableId);
        return Mono.just(ResponseEntity.ok(disposableId));
    }

    private Mono<Order> requestOrderWith(OrderRequest orderRequest, Consumer<Order> afterOrderCompleted){
        return Optional.ofNullable(orderRequest)
                .map(assembler::convertToOrderRequest)
                .map(bobooOrderRequest->exchangeService.requestOrder(bobooOrderRequest, apiKeyDto))
                .map(orderMono->orderMono.map(assembler::convertToOrder)
                    .filter(Objects::nonNull)
                    .doOnSuccess(afterOrderCompleted)
                )
                .orElse(Mono.empty());
    }
}
