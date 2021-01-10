package com.moebius.entropy.domain.order;

import java.math.BigDecimal;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.OrderPosition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class OrderRequest {

    private final Market market;
    private final OrderPosition orderPosition;
    private final BigDecimal price;
    private final BigDecimal volume;
}

