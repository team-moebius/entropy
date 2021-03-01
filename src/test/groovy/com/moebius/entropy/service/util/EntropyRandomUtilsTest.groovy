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
		result >= EXPECTED_MINIMUM
		result <= 3.1452
		result.scale() == EXPECTED_DECIMAL_PLACES

		where:
		DECIMAL_PLACES || EXPECTED_MINIMUM | EXPECTED_DECIMAL_PLACES
		2              || 1.23             | 2
		0              || 1                | 0
	}

	def "Should get random integer"() {
		when:
		def result = entropyRandomUtils.getRandomInteger(3, 10)

		then:
		result >= 3
		result <= 10
	}

	def "Should get random slices"() {
		when:
		def result = entropyRandomUtils.getRandomSlices(BigDecimal.valueOf(VALUE), SLICE_NUMBER, DECIMAL_PLACES)

		then:
		result.size() == SLICE_NUMBER
		result.forEach({
			assert it > BigDecimal.ZERO
			assert it < BigDecimal.valueOf(VALUE)
			assert it.scale() == DECIMAL_PLACES
		})

		where:
		VALUE   | SLICE_NUMBER | DECIMAL_PLACES
		2500.0  | 3            | 2
		2500.0  | 100          | 2
		10000.0 | 100          | 2
		10000.0 | 3            | 2
		2500.0  | 3            | 0
		2500.0  | 100          | 0
		10000.0 | 100          | 0
		10000.0 | 3            | 0

	}
}
