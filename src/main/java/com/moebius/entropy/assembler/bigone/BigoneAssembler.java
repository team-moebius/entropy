package com.moebius.entropy.assembler.bigone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moebius.entropy.dto.exchange.orderbook.bigone.BigoneOrderBookDto;
import com.moebius.entropy.dto.exchange.orderbook.bigone.BigoneOrderBookRequestDto;
import com.moebius.entropy.util.SymbolUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class BigoneAssembler {
	private final static int REQUEST_ID_LENGTH = 6;

	private final ObjectMapper objectMapper;

	public String assembleOrderBookPayload(String symbol) {
		try {
			return objectMapper.writeValueAsString(BigoneOrderBookRequestDto.builder()
				.requestId(RandomStringUtils.randomAlphanumeric(REQUEST_ID_LENGTH))
				.subscribeMarketDepthRequest(BigoneOrderBookRequestDto.DepthRequest.builder()
					.market(SymbolUtil.addDashBeforeBaseCurrency(symbol))
					.build())
				.build());
		} catch (JsonProcessingException e) {
			log.warn("[Bigone] Failed to processing json.", e);
			return StringUtils.EMPTY;
		}
	}

	public BigoneOrderBookDto assembleOrderBookDto(WebSocketMessage message) {
		try {
			return objectMapper.readValue(message.getPayloadAsText(), BigoneOrderBookDto.class);
		} catch (JsonProcessingException e) {
			log.warn("[Bigone] Failed to processing json.", e);
			return null;
		}
	}
}
