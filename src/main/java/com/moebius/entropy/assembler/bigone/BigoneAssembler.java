package com.moebius.entropy.assembler.bigone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneOrderRequestDto;
import com.moebius.entropy.dto.exchange.orderbook.bigone.BigoneOrderBookDto;
import com.moebius.entropy.dto.exchange.orderbook.bigone.BigoneOrderBookRequestDto;
import com.moebius.entropy.util.SymbolUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class BigoneAssembler {
	private final static int REQUEST_ID_LENGTH = 6;
	private final static String OPEN_ORDER_LIMIT = "200";

	private final ObjectMapper objectMapper;

	public MultiValueMap<String, String> assembleOpenOrdersQueryParams(String symbol) {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("asset_pair_name", SymbolUtil.addDashBeforeBaseCurrency(symbol));
		queryParams.add("limit", OPEN_ORDER_LIMIT);
		return queryParams;
	}

	public String assembleOrderBookPayload(String symbol) {
		try {
			return objectMapper.writeValueAsString(BigoneOrderBookRequestDto.builder()
				.requestId(RandomStringUtils.randomAlphanumeric(REQUEST_ID_LENGTH))
				.subscribeMarketDepthRequest(BigoneOrderBookRequestDto.DepthRequest.builder()
					.market(SymbolUtil.addDashBeforeBaseCurrency(symbol))
					.build())
				.build());
		} catch (JsonProcessingException e) {
			log.error("[Bigone] Failed to processing json.", e);
			return StringUtils.EMPTY;
		}
	}

	public BigoneOrderBookDto assembleOrderBookDto(WebSocketMessage message) {
		try {
			return objectMapper.readValue(message.getPayloadAsText(), BigoneOrderBookDto.class);
		} catch (JsonProcessingException e) {
			log.error("[Bigone] Failed to processing json.", e);
			return null;
		}
	}

	public String assembleOrderRequestBodyValue(BigoneOrderRequestDto dto) {
		try {
			return objectMapper.writeValueAsString(dto.toBuilder()
				.symbol(SymbolUtil.addDashBeforeBaseCurrency(dto.getSymbol()))
				.build());
		} catch (JsonProcessingException e) {
			log.error("[Bigone] Failed to processing json.", e);
			return StringUtils.EMPTY;
		}
	}
}
