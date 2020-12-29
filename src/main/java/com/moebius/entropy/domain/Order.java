package com.moebius.entropy.domain;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Order {
    private final String orderId;
    private final String symbol;
    private final Exchange exchange;
    private final OrderType orderType;
    private final BigDecimal price;
    private final BigDecimal volume;
}
