package com.moebius.entropy.service.util

import com.moebius.entropy.util.EntropyRandomUtils
import spock.lang.Specification
import spock.lang.Subject

class EntropyRandomUtilsTest extends Specification {
	@Subject
	def entropyRandomUtils = new EntropyRandomUtils()

	def "Should get random decimal"() {
		when:
		def result = entropyRandomUtils.getRandomDecimal(1.2345, 3.1452, DECIMAL_PLACES)

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

	def "Should get random integer"() {
		when:
		def result = entropyRandomUtils.getRandomInteger(3, 10)

		then:
		result >= 3
		result <= 10
	}
}
