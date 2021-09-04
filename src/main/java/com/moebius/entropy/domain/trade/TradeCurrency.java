package com.moebius.entropy.domain.trade;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TradeCurrency {
    public static final TradeCurrency USDT = new TradeCurrency(new BigDecimal("0.01"), "USDT");
    public static final TradeCurrency DETAILED_USDT = new TradeCurrency(new BigDecimal("0.0001"), "DETAILED_USDT");
    public static final TradeCurrency KRW = new TradeCurrency(BigDecimal.ONE, "KRW");

    private final BigDecimal priceUnit;
    private final String currencyName;

    public String name(){
        return currencyName;
    }
}
