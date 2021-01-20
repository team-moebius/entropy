package com.moebius.entropy.domain.order;

import java.math.BigDecimal;

import com.moebius.entropy.domain.Market;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class OrderRequest {

    private final Market market;
    private final OrderPosition orderPosition;
    private final BigDecimal price;
    private final BigDecimal volume;
}

