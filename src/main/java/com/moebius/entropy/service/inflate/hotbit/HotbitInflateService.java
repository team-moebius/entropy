package com.moebius.entropy.service.inflate.hotbit;

import com.moebius.entropy.assembler.hotbit.HotbitAssembler;
import com.moebius.entropy.configuration.HotbitProperties;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.inflate.InflateService;
import com.moebius.entropy.service.tradewindow.TradeWindowChangeEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotbitInflateService implements InflateService {
    private final static String ORDER_INFLATION_DISPOSABLE_ID_FORMAT = "HOTBIT-%s-ORDER-INFLATION";

    private final WebSocketClient webSocketClient;
    private final HotbitAssembler assembler;
    private final TradeWindowChangeEventListener tradeWindowChangeEventListener;
    private final DisposableOrderRepository disposableOrderRepository;
    private final HotbitProperties configuration;

    @Override
    public void inflateOrdersByOrderBook(String symbol) {
        log.info("[{}}] Start to inflate orders of {} by order book.", getExchange(), symbol);
        disposableOrderRepository.get(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol))
                .forEach(Disposable::dispose);

        var webSocketConfig = configuration.getWebSocket();

        var disposable = webSocketClient.execute(URI.create(webSocketConfig.getUri()),
                        session -> session.send(Mono.just(session.textMessage(assembler.assembleOrderBookPayload(symbol))))
                                .thenMany(session.receive())
                                .subscribeOn(Schedulers.boundedElastic())
                                .timeout(Duration.ofMillis(webSocketConfig.getTimeout()), Schedulers.boundedElastic())
                                .map(assembler::assembleOrderBookDto)
                                .doOnNext(tradeWindowChangeEventListener::inflateOrdersOnTradeWindowChange)
                                .then()
                                .doOnError(exception -> log.error("[{}}] Failed to inflate orders of {} by order book.", getExchange(), symbol, exception))
                                .doOnTerminate(() -> {
                                    log.error("[{}}] Terminated order inflation of {}, retry inflation ...", getExchange(), symbol);
                                    inflateOrdersByOrderBook(symbol);
                                }))
                .subscribe();
        disposableOrderRepository.set(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol), disposable);
    }

    @Override
    public Exchange getExchange() {
        return Exchange.HOTBIT;
    }
}
