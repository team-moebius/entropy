package com.moebius.entropy.dto.exchange.order.boboo;

import com.moebius.entropy.domain.order.OrderStatus;
import lombok.*;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BobooCancelResponse {
    private long exchangeId;
    private String symbol;
    private String clientOrderId;
    private String orderId;
    private OrderStatus status;
}
