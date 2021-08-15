package com.moebius.entropy.service.exchange

import com.fasterxml.jackson.databind.ObjectMapper
import com.moebius.entropy.assembler.BobooAssembler
import com.moebius.entropy.domain.order.ApiKey
import com.moebius.entropy.dto.exchange.order.boboo.*
import com.moebius.entropy.repository.DisposableOrderRepository
import com.moebius.entropy.service.tradewindow.BobooTradeWindowChangeEventListener
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.reactive.ClientHttpRequest
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.client.WebSocketClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

import java.nio.charset.Charset
import java.util.function.Function

@SuppressWarnings('GroovyAccessibility')
class BobooExchangeServiceTestSpec extends Specification {
    def uriSpec = Mock(WebClient.RequestHeadersUriSpec)
    def headersSpec = Mock(WebClient.RequestHeadersSpec)
    def responseSpec = Mock(WebClient.ResponseSpec)
    def webClient = Mock(WebClient)
    def bobooAssembler = Mock(BobooAssembler)
    def disposableOrderRepository = Mock(DisposableOrderRepository)
    def objectMapper = new ObjectMapper()

    @Subject
    def bobooService = new BobooExchangeService(
            webClient, bobooAssembler, disposableOrderRepository, objectMapper
    )

    def "Should get open orders"() {
        given:
        bobooService.authHeaderName = "X-BH-APIKEY"
        def apiKeyDto = Stub(ApiKey) {
            getAccessKey() >> "testAccessKey"
            getSecretKey() >> "testSecretKey"
        }

        1 * webClient.get() >> uriSpec
        1 * uriSpec.uri(_ as Function<UriBuilder, URI>) >> headersSpec
        1 * headersSpec.header("X-BH-APIKEY", "testAccessKey") >> headersSpec
        1 * headersSpec.retrieve() >> responseSpec
        1 * responseSpec.bodyToFlux(BobooOpenOrderDto.class) >> Flux.just(BobooOpenOrderDto.builder().build())

        expect:
        StepVerifier.create(bobooService.getOpenOrders("GTAXUSDT", apiKeyDto))
                .assertNext({ it instanceof BobooOpenOrderDto })
                .verifyComplete()
    }

    def "Should request new order"() {
        bobooService.authHeaderName = "X-BH-APIKEY"
        def apiKeyDto = Stub(ApiKey) {
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
                .assertNext({ it instanceof BobooOrderResponseDto })
                .verifyComplete()
    }

    def "Should request to cancel open order"() {
        bobooService.authHeaderName = "X-BH-APIKEY"
        def apiKeyDto = Stub(ApiKey) {
            getAccessKey() >> "testAccessKey"
            getSecretKey() >> "testSecretKey"
        }
        def cancelRequest = Mock(BobooCancelRequestDto)
        1 * bobooAssembler.assembleCancelRequestQueryParam(cancelRequest) >> new LinkedMultiValueMap<String, String>([:])
        1 * bobooAssembler.assembleCancelRequestBodyValue(_, apiKeyDto) >> new LinkedMultiValueMap<String, String>([:])

        def requestBodyUriSpec = Mock(WebClient.RequestBodyUriSpec)
        def requestBodySpec = Mock(WebClient.RequestBodySpec)

        1 * webClient.method(HttpMethod.DELETE) >> requestBodyUriSpec
        1 * requestBodyUriSpec.uri(_ as Function<UriBuilder, URI>) >> requestBodyUriSpec
        1 * requestBodyUriSpec.header("X-BH-APIKEY", "testAccessKey") >> requestBodySpec
        1 * requestBodySpec.body(_ as BodyInserter<MultiValueMap<String, String>, ClientHttpRequest>) >> requestBodySpec
        1 * requestBodySpec.retrieve() >> responseSpec
        1 * responseSpec.bodyToMono(BobooCancelResponseDto.class) >> Mono.just(BobooCancelResponseDto.builder().build())

        expect:
        StepVerifier.create(bobooService.cancelOrder(cancelRequest, apiKeyDto))
                .assertNext({ it instanceof BobooCancelResponseDto })
                .verifyComplete()
    }

    def "Error due to fulfilled order while request to cancel open order"() {
        bobooService.authHeaderName = "X-BH-APIKEY"
        def apiKeyDto = Stub(ApiKey) {
            getAccessKey() >> "testAccessKey"
            getSecretKey() >> "testSecretKey"
        }
        def cancelRequest = Mock(BobooCancelRequestDto)
        1 * bobooAssembler.assembleCancelRequestQueryParam(cancelRequest) >> new LinkedMultiValueMap<String, String>([:])
        1 * bobooAssembler.assembleCancelRequestBodyValue(_, apiKeyDto) >> new LinkedMultiValueMap<String, String>([:])

        def requestBodyUriSpec = Mock(WebClient.RequestBodyUriSpec)
        def requestBodySpec = Mock(WebClient.RequestBodySpec)

        1 * webClient.method(HttpMethod.DELETE) >> requestBodyUriSpec
        1 * requestBodyUriSpec.uri(_ as Function<UriBuilder, URI>) >> requestBodyUriSpec
        1 * requestBodyUriSpec.header("X-BH-APIKEY", "testAccessKey") >> requestBodySpec
        1 * requestBodySpec.body(_ as BodyInserter<MultiValueMap<String, String>, ClientHttpRequest>) >> requestBodySpec
        1 * requestBodySpec.retrieve() >> responseSpec

        1 * responseSpec.bodyToMono(BobooCancelResponseDto.class) >> Mono.error(new WebClientResponseException.BadRequest(
                "Bad Request", HttpHeaders.EMPTY, "{\"code\":-1142,\"msg\":\"Order has been canceled\"}".getBytes("UTF-8"), Charset.defaultCharset(), null
        ))

        expect:
        StepVerifier.create(bobooService.cancelOrder(cancelRequest, apiKeyDto))
                .assertNext({ it instanceof BobooCancelResponseDto })
                .verifyComplete()

        where:
        errorMsg << [
                "{\"code\":-1142,\"msg\":\"Order has been canceled\"}",
                "{\"code\":-1139,\"msg\":\"Order has been filled.\"}"
        ]
    }

    def "Other error while request to cancel open order"() {
        bobooService.authHeaderName = "X-BH-APIKEY"
        def apiKeyDto = Stub(ApiKey) {
            getAccessKey() >> "testAccessKey"
            getSecretKey() >> "testSecretKey"
        }
        def cancelRequest = Mock(BobooCancelRequestDto)
        1 * bobooAssembler.assembleCancelRequestQueryParam(cancelRequest) >> new LinkedMultiValueMap<String, String>([:])
        1 * bobooAssembler.assembleCancelRequestBodyValue(_, apiKeyDto) >> new LinkedMultiValueMap<String, String>([:])

        def requestBodyUriSpec = Mock(WebClient.RequestBodyUriSpec)
        def requestBodySpec = Mock(WebClient.RequestBodySpec)

        1 * webClient.method(HttpMethod.DELETE) >> requestBodyUriSpec
        1 * requestBodyUriSpec.uri(_ as Function<UriBuilder, URI>) >> requestBodyUriSpec
        1 * requestBodyUriSpec.header("X-BH-APIKEY", "testAccessKey") >> requestBodySpec
        1 * requestBodySpec.body(_ as BodyInserter<MultiValueMap<String, String>, ClientHttpRequest>) >> requestBodySpec
        1 * requestBodySpec.retrieve() >> responseSpec

        1 * responseSpec.bodyToMono(BobooCancelResponseDto.class) >> Mono.error(error)

        expect:
        StepVerifier.create(bobooService.cancelOrder(cancelRequest, apiKeyDto))
                .expectError(WebClientResponseException)
                .verify()

        where:
        error << [
                new WebClientResponseException.BadRequest(
                        "Bad Request", HttpHeaders.EMPTY, "{\"code\":-11423,\"msg\":\"SomeOtherError\"}".getBytes("UTF-8"), Charset.defaultCharset(), null
                ),
                new WebClientResponseException.BadRequest(
                        "Bad Request", HttpHeaders.EMPTY, "NonJsonParsableError".getBytes("UTF-8"), Charset.defaultCharset(), null
                ),
                new WebClientResponseException.InternalServerError(
                        "Bad Request", HttpHeaders.EMPTY, "{\"code\":-1142,\"msg\":\"Order has been canceled\"}".getBytes("UTF-8"), Charset.defaultCharset(), null
                ),
        ]
    }
}
