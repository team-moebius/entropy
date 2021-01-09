package com.moebius.entropy.domain;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class OrderRequest {

    private final Market market;
    private final OrderType orderType;
    private final BigDecimal price;
    private final BigDecimal volume;
}

