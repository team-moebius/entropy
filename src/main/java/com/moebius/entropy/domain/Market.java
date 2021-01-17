package com.moebius.entropy.domain;

import com.moebius.entropy.domain.trade.TradeCurrency;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
@Getter
public class Market {

    private final Exchange exchange;
    private final String symbol;
    private final TradeCurrency tradeCurrency;

}
