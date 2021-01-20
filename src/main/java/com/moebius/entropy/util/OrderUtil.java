package com.moebius.entropy.util;

import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderSide;

public class OrderUtil {
    public static OrderSide resolveFromOrderPosition(OrderPosition orderPosition){
        if (OrderPosition.ASK.equals(orderPosition)){
            return OrderSide.SELL;
        } else {
            return OrderSide.BUY;
        }
    }
    public static OrderPosition resolveFromOrderSide(OrderSide orderSide){
        if (OrderSide.SELL.equals(orderSide)){
            return OrderPosition.ASK;
        } else {
            return OrderPosition.BID;
        }
    }
}
