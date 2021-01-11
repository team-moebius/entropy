package com.moebius.entropy.dto.order;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.DummyOrderConfig;
import lombok.*;

@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DividedDummyOrderDto {
	private Market market;
	private InflationConfig inflationConfig;
	private DummyOrderConfig askOrderConfig;
	private DummyOrderConfig bidOrderConfig;
}
