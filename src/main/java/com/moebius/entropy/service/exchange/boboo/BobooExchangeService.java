package com.moebius.entropy.service.exchange.boboo;

import com.moebius.entropy.assembler.BobooAssembler;
import com.moebius.entropy.dto.exchange.order.ApiKeyDto;
import com.moebius.entropy.dto.exchange.order.boboo.*;
import com.moebius.entropy.service.exchange.ExchangeService;
import com.moebius.entropy.service.tradewindow.BobooTradeWindowChangeEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@SuppressWarnings("unused")
@Slf4j
@Service
@RequiredArgsConstructor
public class BobooExchangeService implements ExchangeService<
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
	@Value("${exchange.boboo.rest.request-orders}")
	private String requestOrderPath;
	@Value("${exchange.boboo.rest.cancel-orders}")
	private String cancelOrderPath;

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
		MultiValueMap<String, String> queryParam = bobooAssembler.assembleCancelRequestQueryParam(cancelRequest);
		MultiValueMap<String, String> bodyValue = bobooAssembler.assembleCancelRequestBodyValue(queryParam, apiKey);

		return webClient.method(HttpMethod.DELETE)
				.uri(uriBuilder -> uriBuilder.scheme(scheme)
						.host(host)
						.path(cancelOrderPath)
						.queryParams(queryParam)
						.build())
				.header(authHeaderName, apiKey.getAccessKey())
				.body(BodyInserters.fromFormData(bodyValue))
				.retrieve()
				.bodyToMono(BobooCancelResponse.class);
	}

	@Override
	public Mono<BobooOrderResponseDto> requestOrder(BobooOrderRequestDto orderRequest, ApiKeyDto apiKey) {
		MultiValueMap<String, String> queryParam = bobooAssembler.assembleOrderRequestQueryParam(orderRequest);
		MultiValueMap<String, String> bodyValue = bobooAssembler.assembleOrderRequestBodyValue(queryParam, apiKey);

		return webClient.post()
				.uri(uriBuilder -> uriBuilder.scheme(scheme)
						.host(host)
						.path(requestOrderPath)
						.queryParams(queryParam)
						.build())
				.header(authHeaderName, apiKey.getAccessKey())
				.body(BodyInserters.fromFormData(bodyValue))
				.retrieve()
				.bodyToMono(BobooOrderResponseDto.class);
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
				.doOnTerminate(() -> getAndLogOrderBook(symbol))
				.then())
			.subscribe();
	}
}
