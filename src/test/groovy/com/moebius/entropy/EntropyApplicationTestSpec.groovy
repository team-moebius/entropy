package com.moebius.entropy

import com.moebius.entropy.service.exchange.BobooService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import spock.lang.Ignore
import spock.lang.Specification

@SpringBootTest(classes = EntropyApplication)
class EntropyApplicationTestSpec extends Specification {
	def bobooService = Mock(BobooService)
	def exchangeServices = [bobooService]
	def applicationReadyEvent = Stub(ApplicationReadyEvent)

	@Autowired
	ApplicationContext context

	@Ignore
	def "Should load spring boot context"() {
		expect:
		context != null
	}

	def "Should execute exchange service's method on application start"() {
		given:
		def entropyApplication = new EntropyApplication(exchangeServices)

		when:
		entropyApplication.onApplicationEvent(applicationReadyEvent)

		then:
		1 * bobooService.getAndLogOrderBook(_ as String)
	}
}