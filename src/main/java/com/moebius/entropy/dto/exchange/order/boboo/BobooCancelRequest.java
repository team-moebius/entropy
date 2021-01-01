package com.moebius.entropy.dto.exchange.order.boboo;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BobooCancelRequest {
    private String orderId;
}
