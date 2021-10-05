package com.moebius.entropy.assembler.bigone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneOrderRequestDto;
import com.moebius.entropy.util.SymbolUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class BigoneAssembler {
	private final static String ORDER_LIMIT = "200";

	private final ObjectMapper objectMapper;

	public MultiValueMap<String, String> assembleOpenOrdersQueryParams(String symbol) {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("asset_pair_name", SymbolUtil.addDashBeforeBaseCurrency(symbol));
		queryParams.add("limit", ORDER_LIMIT);
		return queryParams;
	}

	public MultiValueMap<String, String> assembleOrderBookQueryParams() {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("limit", ORDER_LIMIT);
		return queryParams;
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
