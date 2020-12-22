package com.moebius.entropy.service.exchange;

import com.moebius.entropy.assembler.BobooAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class BobooService implements ExchangeService {
	@Value("${exchange.boboo.websocket.uri}")
	private String uri;

	private final WebSocketClient webSocketClient;
	private final BobooAssembler bobooAssembler;

	public void getAndLogOrderBook(String symbol) {
		webSocketClient.execute(URI.create(uri),
			session -> session.send(Mono.just(session.textMessage(bobooAssembler.assembleOrderBookPayload(symbol))))
				.thenMany(session.receive()
					.map(bobooAssembler::assembleOrderBookDto))
				.doOnNext(bobooOrderBookDto -> log.info("[Boboo] Succeeded in subscribing order book. [{}]", bobooOrderBookDto))
				.then()
				.doOnTerminate(() -> getAndLogOrderBook(symbol)))
			.subscribe();
	}
}
