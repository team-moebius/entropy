package com.moebius.entropy.assembler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moebius.entropy.dto.exchange.order.ApiKeyDto;
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto;
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookRequestDto;
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
	private Map<String, String> params;
	private final ObjectMapper objectMapper;

	public MultiValueMap<String, String> assembleOpenOrdersQueryParams(String symbol, ApiKeyDto apiKey) {
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
			log.warn("[Boboo] [Assemble] Failed to processing json.", e);
			return EMPTY;
		}
	}

	public BobooOrderBookDto assembleOrderBookDto(WebSocketMessage message) {
		try {
			return objectMapper.readValue(message.getPayloadAsText(), BobooOrderBookDto.class);
		} catch (JsonProcessingException e) {
			log.warn("[Boboo] [Assemble] Failed to processing json.", e);
			return null;
		}
	}
}
