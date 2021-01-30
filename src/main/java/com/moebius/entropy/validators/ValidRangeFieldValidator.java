package com.moebius.entropy.validators;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanWrapperImpl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class ValidRangeFieldValidator implements ConstraintValidator<ValidRangeField, Object> {
    private String startFieldName;
    private String endFieldName;

    @Override
    public void initialize(ValidRangeField constraintAnnotation) {
        startFieldName = constraintAnnotation.fieldRangeStart();
        endFieldName = constraintAnnotation.fieldRangeEnd();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        Number fieldRangeStart = (Number) new BeanWrapperImpl(value)
                .getPropertyValue(startFieldName);
        Number fieldRangeEnd = (Number) new BeanWrapperImpl(value)
                .getPropertyValue(endFieldName);

        if (ObjectUtils.allNotNull(fieldRangeStart, fieldRangeEnd)) {
            //noinspection ConstantConditions
            return BigDecimal.valueOf(fieldRangeEnd.doubleValue())
                    .compareTo(BigDecimal.valueOf(fieldRangeStart.doubleValue())) > 0;
        } else {
            return false;
        }
    }
}
