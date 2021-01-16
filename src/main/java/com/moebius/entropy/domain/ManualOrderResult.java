package com.moebius.entropy.domain;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ManualOrderResult {
    private List<Order> requestedOrders;
    private List<Order> cancelledOrders;
}
