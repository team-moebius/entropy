package com.moebius.entropy

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Symbol
import com.moebius.entropy.service.inflate.BobooInflateService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import spock.lang.Specification

@SpringBootTest(classes = EntropyApplication)
class EntropyApplicationTestSpec extends Specification {
	def inflateService = Mock(BobooInflateService)
	def inflateServices = [inflateService]
	def symbols = [(Exchange.BOBOO): [Symbol.GTAX2USDT, Symbol.MOIUSDT],
				   (Exchange.BIGONE): [Symbol.OAUSDT]]
	def applicationReadyEvent = Stub(ApplicationReadyEvent)

	@Autowired
	ApplicationContext context

	def "Should load spring boot context"() {
		expect:
		context != null
	}

	def "Should execute exchange service's method on application start"() {
		given:
		def entropyApplication = new EntropyApplication(inflateServices, symbols)

		when:
		entropyApplication.onApplicationEvent(applicationReadyEvent)

		then:
		1 * inflateService.getExchange() >> Exchange.BOBOO
		2 * inflateService.inflateOrdersByOrderBook(_ as String)
	}
}
