package com.moebius.entropy.domain.trade;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TradeCurrency {
    USDT(new BigDecimal("0.01")),
    THIRD_DECIMAL_USDT(new BigDecimal("0.001")),
    FOURTH_DECIMAL_USDT(new BigDecimal("0.0001")),
    KRW(BigDecimal.ONE);

    private final BigDecimal priceUnit;
}
