package com.moebius.entropy.service.order.hotbit;

import com.moebius.entropy.assembler.hotbit.HotbitOrderExchangeAssembler;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.Symbol;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitOpenOrderResponseDto;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.exchange.hotbit.HotbitExchangeService;
import com.moebius.entropy.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotbitOrderService implements OrderService {
    private final HotbitExchangeService exchangeService;
    private final HotbitOrderExchangeAssembler assembler;
    private final Map<Exchange, Map<Symbol, ApiKey>> apiKeys;
    private final DisposableOrderRepository disposableOrderRepository;

    @Override
    public Flux<Order> fetchAllOrdersFor(Market market) {
        return exchangeService.getOpenOrders(market.getSymbol(), getApiKeyByMarketSymbol(market))
                .flatMapIterable(o -> o.getResult().getData().getRecords())
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
                .map(cancelOrder -> exchangeService.cancelOrder(cancelOrder, getApiKeyByMarketSymbol(order.getMarket())))
                .map(responseMono -> responseMono.map(response -> order)
                        .doOnError(throwable -> log.error("[OrderCancel] Order cancellation failed. [{}]", order, throwable)))
                .orElse(Mono.empty());
    }

    @Override
    public Mono<ResponseEntity<?>> stopOrder(String disposableId) {
        Optional.ofNullable(disposableOrderRepository.get(disposableId))
                .ifPresent(disposables -> disposables.forEach(Disposable::dispose));

        log.info("Succeeded in stopping order service. [{}]", disposableId);
        return Mono.just(ResponseEntity.ok(disposableId));
    }

    @Override
    public Exchange getExchange() {
        return Exchange.HOTBIT;
    }

    private Mono<Order> requestOrderWith(OrderRequest orderRequest, Consumer<Order> afterOrderCompleted) {
        return Optional.ofNullable(orderRequest)
                .map(assembler::convertToOrderRequest)
                .map(request -> exchangeService.requestOrder(request, getApiKeyByMarketSymbol(orderRequest.getMarket())))
                .map(order -> order.map(assembler::convertToOrder)
                        .filter(Objects::nonNull)
                        .doOnSuccess(afterOrderCompleted)
                )
                .orElse(Mono.empty());
    }

    private ApiKey getApiKeyByMarketSymbol(Market market) {
        return Optional.ofNullable(apiKeys.getOrDefault(market.getExchange(), null))
                .map(symbolApiKeyMap -> symbolApiKeyMap.getOrDefault(Symbol.valueOf(market.getSymbol()), null))
                .orElseThrow();
    }
}
