package com.moebius.entropy.service.exchange.boboo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moebius.entropy.assembler.boboo.BobooAssembler;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.domain.order.OrderStatus;
import com.moebius.entropy.dto.exchange.order.boboo.*;
import com.moebius.entropy.service.exchange.ExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SuppressWarnings("unused")
@Slf4j
@Service
@RequiredArgsConstructor
public class BobooExchangeService implements ExchangeService<
	BobooCancelRequestDto, BobooCancelResponseDto, BobooOrderRequestDto, BobooOrderResponseDto, BobooOpenOrderDto
	> {
	private final WebClient webClient;
	private final BobooAssembler bobooAssembler;
	private final ObjectMapper objectMapper;

	@Value("${exchange.boboo.rest.scheme}")
	private String scheme;
	@Value("${exchange.boboo.rest.host}")
	private String host;
	@Value("${exchange.boboo.rest.auth-header-name}")
	private String authHeaderName;
	@Value("${exchange.boboo.rest.open-orders}")
	private String openOrdersPath;
	@Value("${exchange.boboo.rest.request-orders}")
	private String requestOrderPath;
	@Value("${exchange.boboo.rest.cancel-orders}")
	private String cancelOrderPath;

	@Override
	public Flux<BobooOpenOrderDto> getOpenOrders(String symbol, ApiKey apiKey) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder.scheme(scheme)
				.host(host)
				.path(openOrdersPath)
				.queryParams(bobooAssembler.assembleOpenOrdersQueryParams(symbol, apiKey))
				.build())
			.header(authHeaderName, apiKey.getAccessKey())
			.retrieve()
			.bodyToFlux(BobooOpenOrderDto.class);
	}

	@Override
	public Mono<BobooCancelResponseDto> cancelOrder(BobooCancelRequestDto cancelRequest, ApiKey apiKey) {
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
			.bodyToMono(BobooCancelResponseDto.class)
			.doOnError(exception -> log.error("[BobooExchange] Failed to cancel order. {}",
				((WebClientResponseException) exception).getResponseBodyAsString()))
			//{"code":-1142,"msg":"Order has been canceled"}
			//{"code":-1139,"msg":"Order has been filled."}
			.onErrorResume(WebClientResponseException.BadRequest.class, badRequest -> {
				String responseString = badRequest.getResponseBodyAsString();
				try {
					BobooErrorResponseDto bobooErrorResponse = objectMapper.readValue(responseString, BobooErrorResponseDto.class);
					int code = bobooErrorResponse.getCode();
					if (code == -1142 || code == -1139) {
						return Mono.just(BobooCancelResponseDto.builder()
							.orderId(cancelRequest.getOrderId())
							.clientOrderId(cancelRequest.getOrderId())
							.status(OrderStatus.CANCELED)
							.build());
					}
				} catch (JsonProcessingException e) {
					log.error("[BobooOrderCancel] Failed to parse error response from exchange" + responseString, e);
				}
				return Mono.error(badRequest);

			});
	}

	@Override
	public Mono<BobooOrderResponseDto> requestOrder(BobooOrderRequestDto orderRequest, ApiKey apiKey) {
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

	@Override
	public Exchange getExchange() {
		return Exchange.BOBOO;
	}
}
