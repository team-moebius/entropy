package com.moebius.entropy.util;

import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderSide;

import java.math.BigDecimal;
import java.util.Optional;

public class OrderUtil {
    public static OrderSide resolveFromOrderPosition(OrderPosition orderPosition){
        if (OrderPosition.ASK.equals(orderPosition)){
            return OrderSide.SELL;
        } else {
            return OrderSide.BUY;
        }
    }

    public static OrderPosition resolveFromOrderSide(OrderSide orderSide) {
        if (OrderSide.SELL.equals(orderSide)) {
            return OrderPosition.ASK;
        } else {
            return OrderPosition.BID;
        }
    }

    public static OrderPosition resolveFromOrderSideString(String orderSide) {
        if (OrderSide.SELL.name().equalsIgnoreCase(orderSide)) {
            return OrderPosition.ASK;
        } else {
            return OrderPosition.BID;
        }
    }

    public static BigDecimal calculateRemainedVolume(BigDecimal original, BigDecimal executed) {
        BigDecimal orgVolume = Optional.ofNullable(original).orElse(BigDecimal.ZERO);
        BigDecimal executedVolume = Optional.ofNullable(executed).orElse(BigDecimal.ZERO);

        return Optional.of(orgVolume.subtract(executedVolume))
            .filter(volume->volume.compareTo(BigDecimal.ZERO) > -1)
            .orElse(BigDecimal.ZERO);
    }
}
