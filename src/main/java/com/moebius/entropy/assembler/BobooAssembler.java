package com.moebius.entropy.assembler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moebius.entropy.dto.exchange.orderbook.request.BobooOrderBookRequestDto;
import com.moebius.entropy.dto.exchange.orderbook.response.BobooOrderBookDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;

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
