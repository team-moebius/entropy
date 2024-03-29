package com.moebius.entropy.assembler;

import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;

public interface OrderExchangeAssembler<CANCEL_REQ, ORDER_REQ, ORDER_RES, ORDERS> {
    ORDER_REQ convertToOrderRequest(OrderRequest orderRequest);

    Order convertToOrder(ORDER_RES orderResponse);

    CANCEL_REQ convertToCancelRequest(Order order);

    Order convertExchangeOrder(ORDERS ordersFromExchange);
}
