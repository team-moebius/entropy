package com.moebius.entropy.dto.exchange.order.boboo;

import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderSide;
import com.moebius.entropy.domain.order.TimeInForce;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BobooOrderRequestDto {
    private String symbol;
    private BigDecimal quantity;
    private OrderSide side;
    private OrderPosition type;
    private TimeInForce timeInForce;
    private BigDecimal price;
    private String newClientOrderId;
}
