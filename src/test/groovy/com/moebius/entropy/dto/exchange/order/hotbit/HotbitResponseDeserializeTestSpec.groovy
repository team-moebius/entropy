package com.moebius.entropy.dto.exchange.order.hotbit

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.ClassPathResource
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

class HotbitResponseDeserializeTestSpec extends Specification{
    def objectMapper = new ObjectMapper()

    def "Should deserialize HotbitCancelResponseDto"() {
        expect:
        var payload = Files.readString(Paths.get(new ClassPathResource("hotbit/cancel-order.json").getFile().getPath()))
        payload != null
        def result = objectMapper.readValue(payload, HotbitCancelResponseDto)
        result != null
    }

    def "Should deserialize HotbitOpenOrderResponseDto"() {
        given:
        var payload = Files.readString(Paths.get(new ClassPathResource("hotbit/open-order.json").getFile().getPath()))

        expect:
        payload != null
        def result = objectMapper.readValue(payload, HotbitOpenOrderResponseDto)
        result != null
        result.getResult() != null
        result.getResult().getData().getRecords() != null
    }
}
