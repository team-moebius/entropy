package com.moebius.entropy.assembler;

import com.moebius.entropy.domain.Order;
import com.moebius.entropy.domain.OrderRequest;
import com.moebius.entropy.dto.exchange.order.boboo.*;

public class OrderBobooExchangeAssembler implements OrderExchangeAssembler<BobooCancelRequest, BobooOrderRequestDto, BobooOrderResponseDto, BobooOpenOrdersDto>{
    @Override
    public BobooOrderRequestDto convertToOrderRequest(OrderRequest orderRequest) {
        return null;
    }

    @Override
    public Order convertToOrder(BobooOrderResponseDto orderResponse) {
        return null;
    }

    @Override
    public BobooCancelRequest convertToCancelRequest(Order order) {
        return null;
    }

    @Override
    public Order convertExchangeOrder(BobooOpenOrdersDto ordersFromExchange) {
        return null;
    }
}
