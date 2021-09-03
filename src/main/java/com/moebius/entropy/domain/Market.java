package com.moebius.entropy.domain;

import com.moebius.entropy.domain.trade.TradeCurrency;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public class Market {

    private final Exchange exchange;
    private final Symbol symbol;
    private final TradeCurrency tradeCurrency;
    private final int priceDecimalPosition;
    private final int volumeDecimalPosition;
}
