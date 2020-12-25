package com.moebius.entropy.service.exchange

import com.moebius.entropy.assembler.BobooAssembler
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.Subject

class BobooServiceTestSpec extends Specification {
	def webClient = Mock(WebClient)
	def webSocketClient = Mock(WebSocketClient)
	def bobooAssembler = Mock(BobooAssembler)

	@Subject
	def bobooService = new BobooService(webClient, webSocketClient, bobooAssembler)

	def "Should get and log order book"() {
		when:
		bobooService.websocketUri = "wss://wsapi.boboo.vip/openapi/quote/ws/v1"
		bobooService.getAndLogOrderBook("GTAXUSDT")

		then:
		1 * webSocketClient.execute(_ as URI, _ as WebSocketHandler) >> Mono.empty()
	}
}
