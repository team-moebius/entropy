package com.moebius.entropy.domain.order;

import com.moebius.entropy.domain.Market;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Getter
@ToString
public class OrderRequest {

	private final Market market;
	private final OrderPosition orderPosition;
	private final BigDecimal price;
	private final BigDecimal volume;
}

