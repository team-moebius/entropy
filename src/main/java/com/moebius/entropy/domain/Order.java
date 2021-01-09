package com.moebius.entropy.domain;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Order {
    private final String orderId;
    private final Market market;
    private final OrderPosition orderPosition;
    private final BigDecimal price;
    private final BigDecimal volume;
}
