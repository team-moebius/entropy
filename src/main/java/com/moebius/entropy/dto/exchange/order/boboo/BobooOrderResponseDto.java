package com.moebius.entropy.dto.exchange.order.boboo;

import com.moebius.entropy.domain.order.OrderType;
import com.moebius.entropy.domain.order.OrderSide;
import com.moebius.entropy.domain.order.OrderStatus;
import com.moebius.entropy.domain.order.TimeInForce;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BobooOrderResponseDto {
    private String symbol;
    private String orderId;
    private String clientOrderId;
    private long transactTime;
    private BigDecimal price;
    private BigDecimal origQty;
    private BigDecimal executedQty;
    private OrderStatus status;
    private TimeInForce timeInForce;
    private OrderType type;
    private OrderSide side;
}
