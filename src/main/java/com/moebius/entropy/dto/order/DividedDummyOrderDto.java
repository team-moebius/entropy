package com.moebius.entropy.dto.order;

import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.DummyOrderConfig;
import com.moebius.entropy.dto.MarketDto;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DividedDummyOrderDto {
	private MarketDto market;
	private InflationConfig inflationConfig;
	private DummyOrderConfig askOrderConfig;
	private DummyOrderConfig bidOrderConfig;
}
