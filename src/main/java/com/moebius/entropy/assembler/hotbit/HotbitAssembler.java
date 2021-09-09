package com.moebius.entropy.assembler.hotbit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitCancelRequestDto;
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitOpenOrderRequestDto;
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitRequestOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotbitAssembler {
    private final ObjectMapper objectMapper;

    public MultiValueMap<String, String> assembleOpenOrdersQueryParams(ApiKey apikey, HotbitOpenOrderRequestDto openOrderRequestDto) {
        return getQueryParamsMap(openOrderRequestDto.withAccessKey(apikey.getAccessKey()));
    }

    public MultiValueMap<String, String> assembleCancelOrderQueryParams(ApiKey apikey, HotbitCancelRequestDto cancelRequest) {
        return getQueryParamsMap(cancelRequest.withAccessKey(apikey.getAccessKey()));
    }

    public MultiValueMap<String, String> assembleRequestOrderQueryParams(ApiKey apikey, HotbitRequestOrderDto orderRequest) {
        return getQueryParamsMap(orderRequest.withAccessKey(apikey.getAccessKey()));
    }

    private MultiValueMap<String, String> getQueryParamsMap(Object request) {
        var queryParams = new LinkedMultiValueMap<String, String>();
        var map = objectMapper.convertValue(request, new TypeReference<Map<String, String>>() {
        });
        queryParams.setAll(map);
        queryParams.add("sign", DigestUtils.md5Hex(getQueryString(queryParams)).toUpperCase());
        return queryParams;
    }

    private String getQueryString(MultiValueMap<String, String> queryParams) {
        return UriComponentsBuilder.newInstance()
                .queryParams(queryParams)
                .build().encode()
                .getQuery();
    }
}
