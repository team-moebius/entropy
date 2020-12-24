package com.moebius.entropy.domain;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public class TradeWindow {

    private final List<TradePrice> askPrices;
    private final List<TradePrice> bidPrices;
}
