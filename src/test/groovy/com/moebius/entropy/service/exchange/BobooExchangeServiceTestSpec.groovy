package com.moebius.entropy.service.exchange

import com.moebius.entropy.assembler.BobooAssembler
import com.moebius.entropy.dto.exchange.order.ApiKeyDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooCancelRequest
import com.moebius.entropy.dto.exchange.order.boboo.BobooCancelResponse
import com.moebius.entropy.dto.exchange.order.boboo.BobooOpenOrdersDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooOrderRequestDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooOrderResponseDto
import com.moebius.entropy.service.exchange.boboo.BobooExchangeService
import com.moebius.entropy.service.tradewindow.BobooTradeWindowChangeEventListener
import org.springframework.http.HttpMethod
import org.springframework.http.client.reactive.ClientHttpRequest
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserter
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

class BobooExchangeServiceTestSpec extends Specification {
    def uriSpec = Mock(WebClient.RequestHeadersUriSpec)
    def headersSpec = Mock(WebClient.RequestHeadersSpec)
    def responseSpec = Mock(WebClient.ResponseSpec)
    def webClient = Mock(WebClient)
    def webSocketClient = Mock(WebSocketClient)
    def bobooAssembler = Mock(BobooAssembler)
    def eventListener = Mock(BobooTradeWindowChangeEventListener)

    @Subject
    def bobooService = new BobooExchangeService(webClient, webSocketClient, bobooAssembler, eventListener)

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
        bobooService.getAndUpdateByOrderBook("GTAXUSDT")

        then:
        1 * webSocketClient.execute(_ as URI, _ as WebSocketHandler) >> Mono.empty()
    }

    def "Should request new order" (){
        bobooService.authHeaderName = "X-BH-APIKEY"
        def apiKeyDto = Stub(ApiKeyDto) {
            getAccessKey() >> "testAccessKey"
            getSecretKey() >> "testSecretKey"
        }
        def orderRequest = Mock(BobooOrderRequestDto)
        1 * bobooAssembler.assembleOrderRequestQueryParam(orderRequest) >> new LinkedMultiValueMap<String, String>([:])
        1 * bobooAssembler.assembleOrderRequestBodyValue(_, apiKeyDto) >> new LinkedMultiValueMap<String, String>([:])

        def requestBodyUriSpec = Mock(WebClient.RequestBodyUriSpec)
        def requestBodySpec = Mock(WebClient.RequestBodySpec)

        1 * webClient.post() >> requestBodyUriSpec
        1 * requestBodyUriSpec.uri(_ as Function<UriBuilder, URI>) >> requestBodyUriSpec
        1 * requestBodyUriSpec.header("X-BH-APIKEY", "testAccessKey") >> requestBodySpec
        1 * requestBodySpec.body(_ as BodyInserter<MultiValueMap<String, String>, ClientHttpRequest>) >> requestBodySpec
        1 * requestBodySpec.retrieve() >> responseSpec
        1 * responseSpec.bodyToMono(BobooOrderResponseDto.class) >> Mono.just(BobooOrderResponseDto.builder().build())

        expect:
        StepVerifier.create(bobooService.requestOrder(orderRequest, apiKeyDto))
                .assertNext({it instanceof BobooOrderResponseDto })
                .verifyComplete()
    }

    def "Should request to cancel open order" (){
        bobooService.authHeaderName = "X-BH-APIKEY"
        def apiKeyDto = Stub(ApiKeyDto) {
            getAccessKey() >> "testAccessKey"
            getSecretKey() >> "testSecretKey"
        }
        def cancelRequest = Mock(BobooCancelRequest)
        1 * bobooAssembler.assembleCancelRequestQueryParam(cancelRequest) >> new LinkedMultiValueMap<String, String>([:])
        1 * bobooAssembler.assembleCancelRequestBodyValue(_, apiKeyDto) >> new LinkedMultiValueMap<String, String>([:])

        def requestBodyUriSpec = Mock(WebClient.RequestBodyUriSpec)
        def requestBodySpec = Mock(WebClient.RequestBodySpec)

        1 * webClient.method(HttpMethod.DELETE) >> requestBodyUriSpec
        1 * requestBodyUriSpec.uri(_ as Function<UriBuilder, URI>) >> requestBodyUriSpec
        1 * requestBodyUriSpec.header("X-BH-APIKEY", "testAccessKey") >> requestBodySpec
        1 * requestBodySpec.body(_ as BodyInserter<MultiValueMap<String, String>, ClientHttpRequest>) >> requestBodySpec
        1 * requestBodySpec.retrieve() >> responseSpec
        1 * responseSpec.bodyToMono(BobooCancelResponse.class) >> Mono.just(BobooCancelResponse.builder().build())

        expect:
        StepVerifier.create(bobooService.cancelOrder(cancelRequest, apiKeyDto))
                .assertNext({it instanceof BobooCancelResponse })
                .verifyComplete()
    }
}
