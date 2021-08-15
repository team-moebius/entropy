package com.moebius.entropy.assembler;

import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.order.OrderType;
import com.moebius.entropy.domain.order.TimeInForce;
import com.moebius.entropy.dto.exchange.order.boboo.*;
import com.moebius.entropy.util.OrderIdUtil;
import com.moebius.entropy.util.OrderUtil;
import com.moebius.entropy.util.SymbolUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class BobooOrderExchangeAssembler implements OrderExchangeAssembler<BobooCancelRequestDto, BobooOrderRequestDto, BobooOrderResponseDto, BobooOpenOrderDto>{
    @Override
    public BobooOrderRequestDto convertToOrderRequest(OrderRequest orderRequest) {
        return Optional.ofNullable(orderRequest)
                .map(request->BobooOrderRequestDto.builder()
                        .symbol(orderRequest.getMarket().getSymbol())
                        .quantity(orderRequest.getVolume())
                        .side(OrderUtil.resolveFromOrderPosition(orderRequest.getOrderPosition()))
                        .type(OrderType.LIMIT)
                        .timeInForce(TimeInForce.GTC)
                        .price(orderRequest.getPrice())
                        .newClientOrderId(OrderIdUtil.generateOrderId())
                        .build()
                )
                .orElse(null);
    }

    @Override
    public Order convertToOrder(BobooOrderResponseDto orderResponse) {
        return Optional.ofNullable(orderResponse)
                .map(responseDto->new Order(
                        orderResponse.getOrderId(),
                        SymbolUtil.marketFromSymbol(orderResponse.getSymbol()),
                        OrderUtil.resolveFromOrderSide(orderResponse.getSide()),
                        orderResponse.getPrice(),
                        calculateRemainVolume(orderResponse.getOrigQty(), orderResponse.getExecutedQty())
                ))
                .orElse(null);
    }

    @Override
    public BobooCancelRequestDto convertToCancelRequest(Order order) {
        return Optional.ofNullable(order)
                .map(cancelTarget-> BobooCancelRequestDto.builder()
                        .orderId(cancelTarget.getOrderId())
                        .build()
                )
                .orElse(null);
    }

    @Override
    public Order convertExchangeOrder(BobooOpenOrderDto ordersFromExchange) {
        return Optional.ofNullable(ordersFromExchange)
                .map(bobooOpenOrdersDto -> new Order(
                        bobooOpenOrdersDto.getInternalId(),
                        SymbolUtil.marketFromSymbol(bobooOpenOrdersDto.getSymbol()),
                        OrderUtil.resolveFromOrderSide(bobooOpenOrdersDto.getOrderSide()),
                        bobooOpenOrdersDto.getPrice(),
                        calculateRemainVolume(bobooOpenOrdersDto.getOriginalQuantity(), bobooOpenOrdersDto.getExecutedQuantity())
                ))
                .orElse(null);
    }
    private BigDecimal calculateRemainVolume(float original, float executed){
        return calculateRemainVolume(BigDecimal.valueOf(original), BigDecimal.valueOf(executed));
    }

    private BigDecimal calculateRemainVolume(BigDecimal original, BigDecimal executed){
        BigDecimal orgVolume = Optional.ofNullable(original).orElse(BigDecimal.ZERO);
        BigDecimal executedVolume = Optional.ofNullable(executed).orElse(BigDecimal.ZERO);

        return Optional.of(orgVolume.subtract(executedVolume))
                .filter(volume->volume.compareTo(BigDecimal.ZERO) > -1)
                .orElse(BigDecimal.ZERO);
    }
}
