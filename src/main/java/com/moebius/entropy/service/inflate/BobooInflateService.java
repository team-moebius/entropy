package com.moebius.entropy.service.inflate;

import com.moebius.entropy.assembler.BobooAssembler;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.tradewindow.BobooTradeWindowChangeEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class BobooInflateService implements InflateService {
	private final static String ORDER_INFLATION_DISPOSABLE_ID_FORMAT = "BOBOO-%s-ORDER-INFLATION";

	private final WebSocketClient webSocketClient;
	private final BobooAssembler bobooAssembler;
	private final BobooTradeWindowChangeEventListener tradeWindowEventListener;
	private final DisposableOrderRepository disposableOrderRepository;

	@Value("${exchange.boboo.websocket.uri}")
	private String webSocketUri;
	@Value("${exchange.boboo.websocket.timeout}")
	private long timeout;

	@Override
	public void inflateOrdersByOrderBook(String symbol) {
		log.info("[BobooExchange] Start to inflate orders of {} by order book.", symbol);
		disposableOrderRepository.get(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol))
			.forEach(Disposable::dispose);

		Disposable disposable = webSocketClient.execute(URI.create(webSocketUri),
			session -> session.send(Mono.just(session.textMessage(bobooAssembler.assembleOrderBookPayload(symbol))))
				.thenMany(session.receive())
				.subscribeOn(Schedulers.single())
				.timeout(Duration.ofMillis(timeout), Schedulers.single())
				.map(bobooAssembler::assembleOrderBookDto)
				.doOnNext(tradeWindowEventListener::inflateOrdersOnTradeWindowChange)
				.then()
				.doOnError(exception -> log.error("[BobooExchange] Failed to inflate orders of {} by order book.", symbol, exception))
				.doOnTerminate(() -> inflateOrdersByOrderBook(symbol)))
			.subscribe();

		disposableOrderRepository.set(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol), disposable);
	}

	@Override
	public Exchange getExchange() {
		return Exchange.BOBOO;
	}
}
