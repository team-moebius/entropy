package com.moebius.entropy.domain.order;

import java.math.BigDecimal;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.OrderPosition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
@Getter
public class Order {
    private final String orderId;
    private final Market market;
    private final OrderPosition orderPosition;
    private final BigDecimal price;
    private final BigDecimal volume;
}
