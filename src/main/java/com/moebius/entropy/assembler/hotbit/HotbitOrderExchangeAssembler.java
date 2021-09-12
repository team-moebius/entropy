package com.moebius.entropy.assembler.hotbit;

import com.moebius.entropy.assembler.OrderExchangeAssembler;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitCancelRequestDto;
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitOrderDto;
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitRequestOrderDto;
import com.moebius.entropy.dto.exchange.order.hotbit.HotbitRequestOrderResponseDto;
import com.moebius.entropy.util.SymbolUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

import static com.moebius.entropy.domain.order.OrderPosition.ASK;
import static com.moebius.entropy.domain.order.OrderPosition.BID;

@Component
public class HotbitOrderExchangeAssembler implements OrderExchangeAssembler<HotbitCancelRequestDto,
        HotbitRequestOrderDto, HotbitRequestOrderResponseDto, HotbitOrderDto> {
    @Override
    public HotbitRequestOrderDto convertToOrderRequest(OrderRequest orderRequest) {
        return Optional.ofNullable(orderRequest)
                .map(request -> HotbitRequestOrderDto.builder()
                        .symbol(request.getMarket().getSymbol())
                        .side(convertOrderPosition(request.getOrderPosition()))
                        .price(request.getPrice().doubleValue())
                        .amount(request.getVolume().doubleValue())
                        .build())
                .orElse(null);
    }

    private int convertOrderPosition(OrderPosition orderPosition) {
        return orderPosition == ASK ? 1 : 2;
    }

    private OrderPosition convertSide(String side) {
        return Objects.equals(side, "1") ? ASK : BID;
    }

    @Override
    public Order convertToOrder(HotbitRequestOrderResponseDto orderResponse) {
        return Optional.ofNullable(orderResponse)
                .map(HotbitRequestOrderResponseDto::getResult)
                .filter(Objects::nonNull)
                .map(data -> new Order(
                        data.getId(),
                        SymbolUtil.marketFromSymbol(data.getSymbol()),
                        convertSide(data.getSide()),
                        data.getPrice(),
                        data.getAmount()
                ))
                .orElse(null);
    }

    @Override
    public HotbitCancelRequestDto convertToCancelRequest(Order order) {
        return Optional.ofNullable(order)
                .map(cancelTarget -> HotbitCancelRequestDto.builder()
                        .orderId(Integer.parseInt(cancelTarget.getOrderId()))
                        .build())
                .orElse(null);
    }

    @Override
    public Order convertExchangeOrder(HotbitOrderDto openOrderData) {
        return Optional.ofNullable(openOrderData)
                .map(data -> new Order(
                        data.getId(),
                        SymbolUtil.marketFromSymbol(data.getSymbol()),
                        convertSide(data.getSide()),
                        data.getPrice(),
                        data.getAmount()
                )).orElse(null);
    }
}
