package com.moebius.entropy.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Market {

    private final Exchange exchange;
    private final String symbol;
    private final TradeCurrency tradeCurrency;

}
