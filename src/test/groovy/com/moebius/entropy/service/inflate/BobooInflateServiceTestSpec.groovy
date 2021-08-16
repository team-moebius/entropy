package com.moebius.entropy.service.inflate

import com.moebius.entropy.assembler.boboo.BobooAssembler
import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.repository.DisposableOrderRepository
import com.moebius.entropy.service.inflate.boboo.BobooInflateService
import com.moebius.entropy.service.tradewindow.TradeWindowChangeEventListener
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.Disposable
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.Subject

class BobooInflateServiceTestSpec extends Specification {
	def webSocketClient = Mock(WebSocketClient)
	def bobooAssembler = Mock(BobooAssembler)
	def tradeWindowEventListener = Mock(TradeWindowChangeEventListener)
	def disposableOrderRepository = Mock(DisposableOrderRepository)

	@Subject
	def bobooInflateService = new BobooInflateService(webSocketClient, bobooAssembler, tradeWindowEventListener, disposableOrderRepository)

	def "Should inflate orders by order book"() {
		given:
		1 * disposableOrderRepository.get(_ as String) >> [Stub(Disposable)]
		bobooInflateService.webSocketUri = "wss://wsapi.boboo.vip/openapi/quote/ws/v1"

		when:
		bobooInflateService.inflateOrdersByOrderBook("GTAX2USDT")

		then:
		1 * webSocketClient.execute(_ as URI, _ as WebSocketHandler) >> Mono.empty()
		1 * disposableOrderRepository.set(_ as String, _ as Disposable)
	}

	def "Should get exchange"() {
		expect:
		bobooInflateService.getExchange() == Exchange.BOBOO
	}
}
