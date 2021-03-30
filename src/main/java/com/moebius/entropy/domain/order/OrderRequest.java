package com.moebius.entropy.domain.order;

import com.moebius.entropy.domain.Market;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Getter
@Builder
@ToString
public class OrderRequest {

	private final Market market;
	private final OrderPosition orderPosition;
	private final BigDecimal price;
	private final BigDecimal volume;
}

