package com.moebius.entropy.util;

import com.moebius.entropy.domain.OrderPosition;
import com.moebius.entropy.domain.order.OrderSide;

public class OrderUtil {
    public static OrderSide resolveFromOrderPosition(OrderPosition orderPosition){
        if (OrderPosition.ASK.equals(orderPosition)){
            return OrderSide.BUY;
        } else {
            return OrderSide.SELL;
        }
    }
    public static OrderPosition resolveFromOrderSide(OrderSide orderSide){
        if (OrderSide.SELL.equals(orderSide)){
            return OrderPosition.BID;
        } else {
            return OrderPosition.ASK;
        }
    }
}
