package com.moebius.entropy.assembler

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookRequestDto
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto
import org.springframework.util.StringUtils
import org.springframework.web.reactive.socket.WebSocketMessage
import spock.lang.Specification
import spock.lang.Subject

class BobooAssemblerTestSpec extends Specification {
	def objectMapper = Mock(ObjectMapper)

	@Subject
	def bobooAssembler = new BobooAssembler(objectMapper)

	def setup() {
		bobooAssembler.topic = "depth"
		bobooAssembler.event = "sub"
		bobooAssembler.params = ["binary": "false"]
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
}
