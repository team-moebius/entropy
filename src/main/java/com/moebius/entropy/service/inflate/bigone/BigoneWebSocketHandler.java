package com.moebius.entropy.service.inflate.bigone;

import com.moebius.entropy.assembler.bigone.BigoneAssembler;
import com.moebius.entropy.service.tradewindow.TradeWindowChangeEventListener;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.thymeleaf.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BigoneWebSocketHandler implements WebSocketHandler {
	private final BigoneAssembler bigoneAssembler;
	private final TradeWindowChangeEventListener tradeWindowChangeEventListener;

	@Value("${exchange.bigone.websocket.sub-protocol}")
	private String subProtocol;
	@Value("${exchange.bigone.websocket.timeout}")
	private long timeout;
	@Setter
	private String symbol;
	@Setter
	private BigoneInflateService bigoneInflateService;

	@Override
	public List<String> getSubProtocols() {
		return Collections.singletonList(subProtocol);
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		if (StringUtils.isEmpty(symbol)) {
			log.warn("[Bigone] The symbol is empty, please set proper symbol first.");
			return Mono.empty();
		}

		return session.send(Mono.just(session.textMessage(bigoneAssembler.assembleOrderBookPayload(symbol))))
			.thenMany(session.receive())
			.subscribeOn(Schedulers.single())
			.timeout(Duration.ofMillis(timeout), Schedulers.single())
			.map(bigoneAssembler::assembleOrderBookDto)
			.doOnNext(tradeWindowChangeEventListener::inflateOrdersOnTradeWindowChange)
			.then()
			.doOnError(exception -> log.error("[Bigone] Failed to inflate orders of {} by order book.", symbol, exception))
			.doOnTerminate(() -> {
				log.error("[Bigone] Terminated order inflation of {}, retry inflation ...", symbol);
				bigoneInflateService.inflateOrdersByOrderBook(symbol);
			});
	}
}
