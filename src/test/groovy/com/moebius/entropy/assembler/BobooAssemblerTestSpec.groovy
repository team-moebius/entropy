package com.moebius.entropy.assembler

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.moebius.entropy.domain.order.OrderType
import com.moebius.entropy.domain.order.OrderSide
import com.moebius.entropy.domain.order.TimeInForce
import com.moebius.entropy.dto.exchange.order.ApiKeyDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooCancelRequest
import com.moebius.entropy.dto.exchange.order.boboo.BobooOrderRequestDto
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookRequestDto
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.util.StringUtils
import org.springframework.web.reactive.socket.WebSocketMessage
import spock.lang.Specification
import spock.lang.Subject

class BobooAssemblerTestSpec extends Specification {
	def objectMapper = Mock(ObjectMapper)

	@Subject
	def bobooAssembler = new BobooAssembler(objectMapper)

	def receiveTimeWindow = 3000

	def setup() {
		bobooAssembler.topic = "depth"
		bobooAssembler.event = "sub"
		bobooAssembler.params = ["binary": "false"]
		bobooAssembler.maxReceiveTimeWindowInMills = receiveTimeWindow
	}

	def "Should assemble open orders query params"() {
		given:
		def apiKey = Stub(ApiKeyDto) {
			getAccessKey() >> "testAccessKey"
			getSecretKey() >> "testSecretKey"
		}

		when:
		def result = bobooAssembler.assembleOpenOrdersQueryParams("GTAXUSDT", apiKey)

		then:
		result instanceof MultiValueMap
		result.size() == 3
	}

	def "Should assemble order book payload"() {
		when:
		def result = bobooAssembler.assembleOrderBookPayload("GTAXUSDT")

		then:
		1 * objectMapper.writeValueAsString(_ as BobooOrderBookRequestDto) >> "{\"symbol\": \"GTAXUSDT\", \"topic\": \"depth\", \"event\": \"sub\", \"params\": {\"binary\": false}}"

		result != null
		result.contains("GTAXUSDT")
	}

	def "Should return empty string when thrown json processing exception"() {
		when:
		def result = bobooAssembler.assembleOrderBookPayload("GTAXUSDT")

		then:
		1 * objectMapper.writeValueAsString(_ as BobooOrderBookRequestDto) >> { dto -> throw Stub(JsonProcessingException) }

		StringUtils.isEmpty(result)
	}

	def "Should assemble order book dto"() {
		when:
		def result = bobooAssembler.assembleOrderBookDto(Stub(WebSocketMessage))

		then:
		1 * objectMapper.readValue(_ as String, BobooOrderBookDto.class) >> Stub(BobooOrderBookDto)

		result instanceof BobooOrderBookDto
	}

	def "Should null dto when thrown json processing exception"() {
		when:
		def result = bobooAssembler.assembleOrderBookDto(Stub(WebSocketMessage))

		then:
		1 * objectMapper.readValue(_ as String, BobooOrderBookDto.class) >> { message, clazz -> throw Stub(JsonProcessingException) }

		result == null
	}

	def "Should assemble query param for order request"(){
		given:
		def orderRequest = BobooOrderRequestDto.builder()
				.symbol("GTAXUSDT")
				.quantity(BigDecimal.valueOf(11.11))
				.side(OrderSide.BUY)
				.type(OrderType.LIMIT)
				.timeInForce(TimeInForce.GTC)
				.price(BigDecimal.valueOf(123.123))
				.newClientOrderId("some-test-string")
				.build()

		when:
		def queryParam = bobooAssembler.assembleOrderRequestQueryParam(orderRequest)

		then:
		queryParam != null
		queryParam['symbol'][0] == orderRequest.getSymbol()
		queryParam['quantity'][0] == orderRequest.getQuantity().toString()
		queryParam['side'][0] == orderRequest.getSide().name()
		queryParam['type'][0] == orderRequest.getType().name()
		queryParam['timeInForce'][0] == orderRequest.getTimeInForce().name()
		queryParam['price'][0] == orderRequest.getPrice().toString()
		!StringUtils.isEmpty(queryParam['newClientOrderId'])
	}
	def "Should assemble requestBody for order request"(){
		given:
		def queryParam = new LinkedMultiValueMap([
				"symbol": ["GTAXUSDT"], "side": ["BUY"], "type": ["LIMIT"],
				"timeInForce": ["GTC"], "quantity": ["1"], "price": ["0.1"],
		])
		def apiKeyDto = ApiKeyDto.builder()
				.accessKey("test_access_key")
				.secretKey("test_secrey_key")
				.build()

		when:
		def bodyRequest = bobooAssembler.assembleOrderRequestBodyValue(queryParam, apiKeyDto)

		then:
		bodyRequest != null
		!StringUtils.isEmpty(bodyRequest['signature'][0])
		!StringUtils.isEmpty(bodyRequest['timestamp'][0])
		bodyRequest['recvWindow'][0] == receiveTimeWindow.toString()
	}

	def "Should assemble query param for order cancel request"(){
		given:
		def cancelRequest = BobooCancelRequest.builder()
				.orderId("some-test-string")
				.build()

		when:
		def queryParam = bobooAssembler.assembleCancelRequestQueryParam(cancelRequest)

		then:
		queryParam != null
		queryParam['origClientOrderId'][0] == cancelRequest.orderId
	}
	def "Should assemble requestBody for order cancel request"(){
		given:
		def queryParam = new LinkedMultiValueMap(["origClientOrderId": ["some-test-string"],])
		def apiKeyDto = ApiKeyDto.builder()
				.accessKey("test_access_key")
				.secretKey("test_secrey_key")
				.build()

		when:
		def bodyRequest = bobooAssembler.assembleCancelRequestBodyValue(queryParam, apiKeyDto)

		then:
		bodyRequest != null
		!StringUtils.isEmpty(bodyRequest['signature'][0])
		!StringUtils.isEmpty(bodyRequest['timestamp'][0])
		bodyRequest['recvWindow'][0] == receiveTimeWindow.toString()
	}
}
