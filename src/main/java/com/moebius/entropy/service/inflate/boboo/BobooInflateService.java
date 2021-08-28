package com.moebius.entropy.service.inflate.boboo;

import com.moebius.entropy.assembler.boboo.BobooAssembler;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.inflate.InflateService;
import com.moebius.entropy.service.tradewindow.TradeWindowChangeEventListener;
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
	private final TradeWindowChangeEventListener tradeWindowChangeEventListener;
	private final DisposableOrderRepository disposableOrderRepository;

	@Value("${exchange.boboo.websocket.uri}")
	private String webSocketUri;
	@Value("${exchange.boboo.websocket.timeout}")
	private long timeout;

	@Override
	public void inflateOrdersByOrderBook(String symbol) {
		log.info("[Boboo] Start to inflate orders of {} by order book.", symbol);
		disposableOrderRepository.get(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol))
			.forEach(Disposable::dispose);

		Disposable disposable = webSocketClient.execute(URI.create(webSocketUri),
			session -> session.send(Mono.just(session.textMessage(bobooAssembler.assembleOrderBookPayload(symbol))))
				.thenMany(session.receive())
				.subscribeOn(Schedulers.boundedElastic())
				.timeout(Duration.ofMillis(timeout), Schedulers.boundedElastic())
				.map(bobooAssembler::assembleOrderBookDto)
				.doOnNext(tradeWindowChangeEventListener::inflateOrdersOnTradeWindowChange)
				.then()
				.doOnError(exception -> log.error("[Boboo] Failed to inflate orders of {} by order book.", symbol, exception))
				.doOnTerminate(() -> {
					log.error("[Boboo] Terminated order inflation of {}, retry inflation ...", symbol);
					inflateOrdersByOrderBook(symbol);
				}))
			.subscribe();

		disposableOrderRepository.set(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol), disposable);
	}

	@Override
	public Exchange getExchange() {
		return Exchange.BOBOO;
	}
}
