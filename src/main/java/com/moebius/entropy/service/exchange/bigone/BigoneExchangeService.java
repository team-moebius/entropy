package com.moebius.entropy.service.exchange.bigone;

import com.moebius.entropy.assembler.bigone.BigoneAssembler;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneCancelRequestDto;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneCancelResponseDto;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneOpenOrderDto;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneOrderRequestDto;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneOrderResponseDto;
import com.moebius.entropy.dto.exchange.orderbook.bigone.BigoneOrderBookDto;
import com.moebius.entropy.service.exchange.ExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class BigoneExchangeService implements
	ExchangeService<BigoneCancelRequestDto, BigoneCancelResponseDto, BigoneOrderRequestDto, BigoneOrderResponseDto, BigoneOpenOrderDto> {

	private final WebClient webClient;
	private final BigoneJwtService bigoneJwtService;
	private final BigoneAssembler bigoneAssembler;

	@Value("${exchange.bigone.rest.scheme}")
	private String scheme;
	@Value("${exchange.bigone.rest.host}")
	private String host;
	@Value("${exchange.bigone.rest.open-orders}")
	private String openOrdersPath;
	@Value("${exchange.bigone.rest.request-orders}")
	private String requestOrderPath;
	@Value("${exchange.bigone.rest.cancel-orders}")
	private String cancelOrderPath;
	@Value("${exchange.bigone.rest.order-book}")
	private String orderBookPath;

	@Override
	public Flux<BigoneOpenOrderDto> getOpenOrders(String symbol, ApiKey apiKey) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder.scheme(scheme)
				.host(host)
				.path(openOrdersPath)
				.queryParams(bigoneAssembler.assembleOpenOrdersQueryParams(symbol))
				.build())
			.headers(httpHeaders -> httpHeaders.setBearerAuth(bigoneJwtService.create(apiKey)))
			.retrieve()
			.bodyToFlux(BigoneOpenOrderDto.class);
	}

	@Override
	public Mono<BigoneCancelResponseDto> cancelOrder(BigoneCancelRequestDto cancelRequest,
		ApiKey apiKey) {
		return webClient.post()
			.uri(uriBuilder -> uriBuilder.scheme(scheme)
				.host(host)
				.path(cancelOrderPath)
				.build(cancelRequest.getId()))
			.headers(httpHeaders -> httpHeaders.setBearerAuth(bigoneJwtService.create(apiKey)))
			.retrieve()
			.bodyToMono(BigoneCancelResponseDto.class);
	}

	@Override
	public Mono<BigoneOrderResponseDto> requestOrder(BigoneOrderRequestDto orderRequest,
		ApiKey apiKey) {
		return webClient.post()
			.uri(uriBuilder -> uriBuilder.scheme(scheme)
				.host(host)
				.path(requestOrderPath)
				.build())
			.contentType(MediaType.APPLICATION_JSON)
			.headers(httpHeaders -> httpHeaders.setBearerAuth(bigoneJwtService.create(apiKey)))
			.bodyValue(bigoneAssembler.assembleOrderRequestBodyValue(orderRequest))
			.retrieve()
			.bodyToMono(BigoneOrderResponseDto.class);
	}

	@Override
	public Exchange getExchange() {
		return Exchange.BIGONE;
	}

	public Mono<BigoneOrderBookDto> getOrderBook(String symbol) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder.scheme(scheme)
				.host(host)
				.path(orderBookPath)
				.queryParams(bigoneAssembler.assembleOrderBookQueryParams())
				.build(symbol))
			.retrieve()
			.bodyToMono(BigoneOrderBookDto.class);
	}
}
