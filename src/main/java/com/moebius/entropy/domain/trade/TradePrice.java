package com.moebius.entropy.domain.trade;

import java.math.BigDecimal;

import com.moebius.entropy.domain.order.OrderPosition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class TradePrice {

    private final OrderPosition orderPosition;
    private final BigDecimal unitPrice;
    private final BigDecimal volume;
}
