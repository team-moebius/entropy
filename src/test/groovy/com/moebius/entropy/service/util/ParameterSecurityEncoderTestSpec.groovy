package com.moebius.entropy.service.util

import com.moebius.entropy.util.ParameterSecurityEncoder
import groovyjarjarantlr4.v4.misc.OrderedHashMap
import org.apache.commons.collections4.MultiValuedMap
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import spock.lang.Specification

class ParameterSecurityEncoderTestSpec extends Specification {
    def secretKey = "lH3ELTNiFxCQTmi9pPcWWikhsjO04Yoqw3euoHUuOLC3GYBW64ZqzQsiOEHXQS76"

    def "Encode parameters"() {
        given:
        MultiValueMap<String, String> queryParameter = new LinkedMultiValueMap<>(queries)
        MultiValueMap<String, String> bodyValues = new LinkedMultiValueMap<>(bodies)

        when:
        def signature = ParameterSecurityEncoder.encodeParameters(queryParameter, bodyValues, secretKey)
        then:
        signature == desiredSignature

        where:
        queries << [
                ["symbol": ["ETHBTC"], "side": ["BUY"], "type": ["LIMIT"], "timeInForce": ["GTC"], "quantity": ["1"], "price": ["0.1"], "recvWindow": ["5000"], "timestamp": ["1538323200000"],],
                ["symbol": ["ETHBTC"], "side": ["BUY"], "type": ["LIMIT"], "timeInForce": ["GTC"],],
                [:]

        ]
        bodies << [
                [:],
                ["quantity": ["1"], "price": ["0.1"], "recvWindow": ["5000"], "timestamp": ["1538323200000"],],
                ["symbol": ["ETHBTC"], "side": ["BUY"], "type": ["LIMIT"], "timeInForce": ["GTC"], "quantity": ["1"], "price": ["0.1"], "recvWindow": ["5000"], "timestamp": ["1538323200000"],],
        ]
        desiredSignature << [
                '5f2750ad7589d1d40757a55342e621a44037dad23b5128cc70e18ec1d1c3f4c6',
                '885c9e3dd89ccd13408b25e6d54c2330703759d7494bea6dd5a3d1fd16ba3afa',
                '5f2750ad7589d1d40757a55342e621a44037dad23b5128cc70e18ec1d1c3f4c6',
        ]
    }
}
