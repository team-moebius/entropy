package com.moebius.entropy.domain;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class InflationResult {

    private final List<BigDecimal> createdAskOrderPrices;
    private final List<BigDecimal> createdBidOrderPrices;

    private final List<BigDecimal> cancelledAskOrderPrices;
    private final List<BigDecimal> cancelledBidOrderPrices;
}
