package com.moebius.entropy.assembler.boboo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.dto.exchange.order.boboo.BobooCancelRequestDto;
import com.moebius.entropy.dto.exchange.order.boboo.BobooOrderRequestDto;
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto;
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookRequestDto;
import com.moebius.entropy.util.OrderIdUtil;
import com.moebius.entropy.util.ParameterSecurityEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketMessage;

import java.time.Instant;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Slf4j
@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "exchange.boboo.orderbook")
public class BobooAssembler {
	@Value("${exchange.boboo.orderbook.topic}")
	private String topic;
	@Value("${exchange.boboo.orderbook.event}")
	private String event;
	@Value("${exchange.boboo.receiveTimeWindow}")
	private String maxReceiveTimeWindowInMills;
	private Map<String, String> params;
	private final ObjectMapper objectMapper;

	public MultiValueMap<String, String> assembleOpenOrdersQueryParams(String symbol, ApiKey apiKey) {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

		queryParams.add("symbol", symbol);
		queryParams.add("timestamp", String.valueOf(Instant.now().toEpochMilli()));
		queryParams.add("signature", new HmacUtils(HmacAlgorithms.HMAC_SHA_256, apiKey.getSecretKey())
			.hmacHex(queryParams.entrySet().stream()
				.map(p -> p.getKey() + "=" + p.getValue().get(0))
				.reduce((p1, p2) -> p1 + "&" + p2)
				.orElse("")));

		return queryParams;
	}

	public String assembleOrderBookPayload(String symbol) {
		try {
			return objectMapper.writeValueAsString(BobooOrderBookRequestDto.builder()
				.symbol(symbol)
				.topic(topic)
				.event(event)
				.params(params)
				.build());
		} catch (JsonProcessingException e) {
			log.warn("[Boboo] Failed to processing json.", e);
			return EMPTY;
		}
	}

	public BobooOrderBookDto assembleOrderBookDto(WebSocketMessage message) {
		try {
			return objectMapper.readValue(message.getPayloadAsText(), BobooOrderBookDto.class);
		} catch (JsonProcessingException e) {
			log.warn("[Boboo] Failed to processing json.", e);
			return null;
		}
	}

	public MultiValueMap<String, String> assembleOrderRequestQueryParam(BobooOrderRequestDto orderRequest){
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("symbol", orderRequest.getSymbol());
		queryParams.add("quantity", orderRequest.getQuantity().toString());
		queryParams.add("side", orderRequest.getSide().name());
		queryParams.add("type", orderRequest.getType().name());
		queryParams.add("timeInForce", orderRequest.getTimeInForce().name());
		queryParams.add("price", orderRequest.getPrice().toString());
		queryParams.add("newClientOrderId", OrderIdUtil.generateOrderId());
		return queryParams;
	}

	public MultiValueMap<String, String> assembleOrderRequestBodyValue(MultiValueMap<String, String> queryParam,
																	   ApiKey apiKeyDto) {
		MultiValueMap<String, String> requestBody = createBaseBodyValue();

		String signature = ParameterSecurityEncoder.encodeParameters(queryParam, requestBody, apiKeyDto.getSecretKey());
		requestBody.add("signature", signature);

		return requestBody;
	}

	public MultiValueMap<String, String> assembleCancelRequestQueryParam(BobooCancelRequestDto bobooCancelRequest) {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("orderId", bobooCancelRequest.getOrderId());
		return queryParams;
	}

	public MultiValueMap<String, String> assembleCancelRequestBodyValue(MultiValueMap<String, String> queryParam,
																		ApiKey apiKeyDto) {
		MultiValueMap<String, String> requestBody = createBaseBodyValue();

		String signature = ParameterSecurityEncoder.encodeParameters(queryParam, requestBody, apiKeyDto.getSecretKey());
		requestBody.add("signature", signature);

		return requestBody;
	}

	private MultiValueMap<String, String> createBaseBodyValue() {
		MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
		requestBody.add("timestamp", String.valueOf(Instant.now().toEpochMilli()));
		requestBody.add("recvWindow", maxReceiveTimeWindowInMills);
		return requestBody;
	}
}
