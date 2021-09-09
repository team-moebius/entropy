package com.moebius.entropy.assembler.hotbit

import com.fasterxml.jackson.databind.ObjectMapper
import com.moebius.entropy.domain.order.ApiKey
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitCancelRequestDto
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitOpenOrderRequestDto
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitRequestOrderDto
import spock.lang.Specification

class HotbitAssemblerTest extends Specification {
    def assembler = new HotbitAssembler(new ObjectMapper())

    def "AssembleOpenOrdersQueryParams"() {
        given:
        def key = new ApiKey()
        key.setAccessKey("accessKey")
        def request = HotbitOpenOrderRequestDto.builder()
                .symbol("symbol")
                .build()

        expect:
        def result = assembler.assembleOpenOrdersQueryParams(key, request)
        result.containsKey("sign")
        result.get("market") == [request.getSymbol()]
        result.get("api_key") == [key.getAccessKey()]
        result.get("offset") == ["0"]
        result.get("limit") == ["200"]
    }

    def "assembleCancelOrderQueryParams"() {
        given:
        def key = new ApiKey()
        key.setAccessKey("accessKey")
        def cancelRequestDto = HotbitCancelRequestDto.builder()
                .orderId(1)
                .symbol("symbol")
                .build()
        expect:
        def result = assembler.assembleCancelOrderQueryParams(key, cancelRequestDto)
        result.get("market") == [cancelRequestDto.getSymbol()]
        result.get("order_id") as List<String> == ["${cancelRequestDto.getOrderId()}"]
        result.get("api_key") == [key.getAccessKey()]
        result.containsKey("sign")
    }

    def "assembleRequestOrderQueryParams"() {
        given:
        def key = new ApiKey()
        key.setAccessKey("accessKey")
        def requestOrderDto = HotbitRequestOrderDto.builder()
                .symbol("symbol")
                .side(1)
                .amount(10.0)
                .price(100.0)
                .isFee(1)
                .build()
        expect:
        def result = assembler.assembleRequestOrderQueryParams(key, requestOrderDto)
        result.get("market") == [requestOrderDto.getSymbol()]
        result.get("side") == ["${requestOrderDto.getSide()}"]
        result.get("amount") == ["${requestOrderDto.getAmount()}"]
        result.get("price") == ["${requestOrderDto.getPrice()}"]
        result.get("isfee") == ["${requestOrderDto.getIsFee()}"]
        result.get("api_key") == [key.getAccessKey()]
        result.containsKey("sign")
    }
}
