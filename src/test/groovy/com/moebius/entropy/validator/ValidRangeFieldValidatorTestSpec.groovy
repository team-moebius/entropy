package com.moebius.entropy.validator

import spock.lang.Specification

import javax.validation.Validation

class ValidRangeFieldValidatorTestSpec extends Specification {
    @ValidRangeField.List([
            @ValidRangeField(fieldRangeStart = "from", fieldRangeEnd = "end"),
            @ValidRangeField(fieldRangeStart = "intFrom", fieldRangeEnd = "intEnd")
    ])
    class BigDecimalRange {
        BigDecimal from
        BigDecimal end
        int intFrom
        int intEnd

        BigDecimalRange(BigDecimal from, BigDecimal end, int intFrom, int intEnd) {
            this.from = from
            this.end = end
            this.intFrom = intFrom
            this.intEnd = intEnd
        }
    }

    def "Should validate class"() {
        given:
        def validationTarget = new BigDecimalRange(
                from.toBigDecimal(), to.toBigDecimal(), from.toInteger(), to.toInteger()
        )
        def validatorFactory = Validation.buildDefaultValidatorFactory()
        def validator = validatorFactory.getValidator()

        when:
        def violations = validator.validate(validationTarget)

        then:
        violations.isEmpty() == valid

        where:
        from    | to         | valid
        123.123 | 455623.123 | true
        0       | 123        | true
        123     | 0          | false
        0       | 0          | false
        235.12  | 13.2       | false
        -123    | -1254      | false
        -11     | -5         | true

    }
}
