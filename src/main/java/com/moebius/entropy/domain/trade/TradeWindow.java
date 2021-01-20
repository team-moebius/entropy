package com.moebius.entropy.domain.trade;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public class TradeWindow {

    public static final TradeWindow emptyWindow = new TradeWindow(Collections.emptyList(),
        Collections.emptyList());

    private final List<TradePrice> askPrices;
    private final List<TradePrice> bidPrices;
}
