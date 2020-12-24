package com.moebius.entropy.domain;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TradeCurrency {
    USDT(new BigDecimal("0.01")),
    KRW(BigDecimal.ONE);

    private final BigDecimal priceUnit;
}
