package com.moebius.entropy.domain;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TradePrice {

    private final OrderType orderType;
    private final BigDecimal unitPrice;
    private final BigDecimal volume;
}
