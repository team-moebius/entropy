package com.moebius.entropy.assembler.hotbit

import org.springframework.util.LinkedMultiValueMap
import spock.lang.Specification

class HotbitSignatureTest extends Specification {
    def "Generate"() {
        given:
        def apiKey = "6b97d781-5ffd-958f-576d96d0bbebc8c6";
        def secretKey = "de8063ea6e99bc967ba6395d06fabf50";
        var queryParams = new LinkedMultiValueMap<String, String>();
        queryParams.add("api_key", apiKey)
        queryParams.add("assets", "[\"BTC\",\"ETH\"]")

        when:
        def sign = HotbitSignature.generate(secretKey, queryParams)

        then:
        sign == "C88F04701D3349D0A93A0164DC5A4CD9"
    }
}
