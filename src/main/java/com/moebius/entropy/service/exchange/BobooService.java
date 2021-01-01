package com.moebius.entropy.service.exchange;

import com.moebius.entropy.assembler.BobooAssembler;
import com.moebius.entropy.dto.exchange.order.ApiKeyDto;
import com.moebius.entropy.dto.exchange.order.boboo.BobooOpenOrdersDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class BobooService implements ExchangeService {
	@Value("${exchange.boboo.rest.scheme}")
	private String scheme;
	@Value("${exchange.boboo.rest.host}")
	private String host;
	@Value("${exchange.boboo.rest.auth-header-name}")
	private String authHeaderName;
	@Value("${exchange.boboo.rest.open-orders}")
	private String openOrdersPath;
	@Value("${exchange.boboo.websocket.uri}")
	private String websocketUri;

	private final WebClient webClient;
	private final WebSocketClient webSocketClient;
	private final BobooAssembler bobooAssembler;

	public Flux<BobooOpenOrdersDto> getOpenOrders(String symbol, ApiKeyDto apiKey) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder.scheme(scheme)
				.host(host)
				.path(openOrdersPath)
				.queryParams(bobooAssembler.assembleOpenOrdersQueryParams(symbol, apiKey))
				.build())
			.header(authHeaderName, apiKey.getAccessKey())
			.retrieve()
			.bodyToFlux(BobooOpenOrdersDto.class);
	}

	public void getAndLogOrderBook(String symbol) {
		webSocketClient.execute(URI.create(websocketUri),
			session -> session.send(Mono.just(session.textMessage(bobooAssembler.assembleOrderBookPayload(symbol))))
				.thenMany(session.receive()
					.map(bobooAssembler::assembleOrderBookDto))
				.doOnNext(bobooOrderBookDto -> log.info("[Boboo] Succeeded in subscribing order book. [{}]", bobooOrderBookDto))
				.then()
				.doOnTerminate(() -> getAndLogOrderBook(symbol)))
			.subscribe();
	}
}
