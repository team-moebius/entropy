package com.moebius.entropy.service.exchange

import com.moebius.entropy.assembler.BobooAssembler
import com.moebius.entropy.dto.exchange.order.ApiKeyDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooOpenOrdersDto
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.client.WebSocketClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

import java.util.function.Function

class BobooServiceTestSpec extends Specification {
    def uriSpec = Mock(WebClient.RequestHeadersUriSpec)
    def headersSpec = Mock(WebClient.RequestHeadersSpec)
    def responseSpec = Mock(WebClient.ResponseSpec)
    def webClient = Mock(WebClient)
    def webSocketClient = Mock(WebSocketClient)
    def bobooAssembler = Mock(BobooAssembler)

    @Subject
    def bobooService = new BobooService(webClient, webSocketClient, bobooAssembler)

    def "Should get open orders"() {
        given:
        bobooService.authHeaderName = "X-BH-APIKEY"
        def apiKeyDto = Stub(ApiKeyDto) {
            getAccessKey() >> "testAccessKey"
            getSecretKey() >> "testSecretKey"
        }

        1 * webClient.get() >> uriSpec
        1 * uriSpec.uri(_ as Function<UriBuilder, URI>) >> headersSpec
        1 * headersSpec.header("X-BH-APIKEY", "testAccessKey") >> headersSpec
        1 * headersSpec.retrieve() >> responseSpec
        1 * responseSpec.bodyToFlux(BobooOpenOrdersDto.class) >> Flux.just(BobooOpenOrdersDto.builder().build())

		expect:
        StepVerifier.create(bobooService.getOpenOrders("GTAXUSDT", apiKeyDto))
                .assertNext({it instanceof BobooOpenOrdersDto })
                .verifyComplete()
    }

    def "Should get and log order book"() {
        when:
        bobooService.websocketUri = "wss://wsapi.boboo.vip/openapi/quote/ws/v1"
        bobooService.getAndLogOrderBook("GTAXUSDT")

        then:
        1 * webSocketClient.execute(_ as URI, _ as WebSocketHandler) >> Mono.empty()
    }
}
