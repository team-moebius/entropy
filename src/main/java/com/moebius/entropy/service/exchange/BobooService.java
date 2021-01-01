package com.moebius.entropy.service.exchange;

import com.moebius.entropy.assembler.BobooAssembler;
import com.moebius.entropy.dto.exchange.order.*;
import com.moebius.entropy.dto.exchange.order.boboo.*;
import com.moebius.entropy.service.tradewindow.BobooTradeWindowChangeEventListener;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class BobooService implements ExchangeService<
    BobooCancelRequest, BobooCancelResponse, BobooOrderRequestDto, BobooOrderResponseDto, BobooOpenOrdersDto
> {
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
	private final BobooTradeWindowChangeEventListener tradeWindowEventListener;

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

	@Override
	public Mono<BobooCancelResponse> cancelOrder(BobooCancelRequest cancelRequest, ApiKeyDto apiKey) {
		return null;
	}

	@Override
	public Mono<BobooOrderResponseDto> requestOrder(BobooOrderRequestDto orderRequest, ApiKeyDto apiKey) {
		return null;
	}

	public void getAndLogOrderBook(String symbol) {
		webSocketClient.execute(URI.create(websocketUri),
			session -> session.send(
				Mono.just(session.textMessage(bobooAssembler.assembleOrderBookPayload(symbol))))
				.thenMany(session.receive()
					.map(bobooAssembler::assembleOrderBookDto))
				.doOnNext(bobooOrderBookDto -> log
					.info("[Boboo] Succeeded in subscribing order book. [{}]", bobooOrderBookDto))
				.doOnNext(tradeWindowEventListener::onTradeWindowChange)
				.then()
				.doOnTerminate(() -> getAndLogOrderBook(symbol)))
			.subscribe();
	}
}
