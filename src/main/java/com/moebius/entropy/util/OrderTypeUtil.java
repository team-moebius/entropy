package com.moebius.entropy.util;

import com.moebius.entropy.domain.OrderType;
import com.moebius.entropy.domain.order.OrderSide;

public class OrderTypeUtil {
    public static OrderSide resolveFromOrderType(OrderType orderType){
        if (OrderType.ASK.equals(orderType)){
            return OrderSide.BUY;
        } else {
            return OrderSide.SELL;
        }
    }
    public static OrderType resolveFromOrderSide(OrderSide orderSide){
        if (OrderSide.SELL.equals(orderSide)){
            return OrderType.BID;
        } else {
            return OrderType.ASK;
        }
    }
}
