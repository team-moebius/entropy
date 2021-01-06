package com.moebius.entropy.service.order

import spock.lang.Specification
import spock.lang.Subject

class OrderQuantityServiceTest extends Specification {
	@Subject
	def orderQuantityService = new OrderQuantityService()

	def "Should get random quantity"() {
		when:
		def result = orderQuantityService.getRandomQuantity(1.2345, 3.1452, DECIMAL_PLACES)

		then:
		result instanceof BigDecimal
		result >= 1.2345
		result <= 3.1452
		result.scale() == EXPECTED_DECIMAL_PLACES

		where:
		DECIMAL_PLACES | EXPECTED_DECIMAL_PLACES
		2              | 2
		0              | 0
	}
}
