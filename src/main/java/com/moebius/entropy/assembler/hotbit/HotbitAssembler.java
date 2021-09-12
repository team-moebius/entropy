package com.moebius.entropy.assembler.hotbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitCancelRequestDto;
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitOpenOrderRequestDto;
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitRequestOrderDto;
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto;
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookRequestDto;
import com.moebius.entropy.dto.exchange.orderbook.hotbit.HotbitOrderBookRequestDto;
import com.moebius.entropy.dto.exchange.orderbook.hotbit.HotbitOrderBookResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotbitAssembler {
    private final ObjectMapper objectMapper;

    public MultiValueMap<String, String> assembleOpenOrdersQueryParams(ApiKey apikey, HotbitOpenOrderRequestDto openOrderRequestDto) {
        return getQueryParamsMap(apikey.getSecretKey(), openOrderRequestDto.withAccessKey(apikey.getAccessKey()));
    }

    public MultiValueMap<String, String> assembleCancelOrderQueryParams(ApiKey apikey, HotbitCancelRequestDto cancelRequest) {
        return getQueryParamsMap(apikey.getSecretKey(), cancelRequest.withAccessKey(apikey.getAccessKey()));
    }

    public MultiValueMap<String, String> assembleRequestOrderQueryParams(ApiKey apikey, HotbitRequestOrderDto orderRequest) {
        return getQueryParamsMap(apikey.getSecretKey(), orderRequest.withAccessKey(apikey.getAccessKey()));
    }

    public String assembleOrderBookPayload(String symbol) {
        try {
            return objectMapper.writeValueAsString(HotbitOrderBookRequestDto.builder()
                    .symbol(symbol)
                    .priceLevel(1)
                    .pricePrecision(0.1)
                    .build());
        } catch (JsonProcessingException e) {
            log.warn("[{}}] Failed to processing json.", Exchange.HOTBIT, e);
            return EMPTY;
        }
    }

    public HotbitOrderBookResponseDto assembleOrderBookDto(WebSocketMessage message) {
        try {
            return objectMapper.readValue(message.getPayloadAsText(), HotbitOrderBookResponseDto.class);
        } catch (JsonProcessingException e) {
            log.warn("[Boboo] Failed to processing json.", e);
            return null;
        }
    }

    private MultiValueMap<String, String> getQueryParamsMap(String secretKey, Object request) {
        var queryParams = new LinkedMultiValueMap<String, String>();
        var map = objectMapper.convertValue(request, new TypeReference<Map<String, String>>() {
        });
        queryParams.setAll(map);
        queryParams.add("sign", DigestUtils.md5Hex(getQueryString(secretKey, queryParams)).toUpperCase());
        return queryParams;
    }

    private String getQueryString(String secretKey, MultiValueMap<String, String> queryParams) {
        var queryString = UriComponentsBuilder.newInstance()
                .queryParams(queryParams)
                .build().encode()
                .getQuery();

        var orderedQueryString = Arrays.stream(queryString.split("&")).sorted().collect(Collectors.joining("&"));

        return String.format("%s&secret_key=%s", orderedQueryString, secretKey);
    }
}
