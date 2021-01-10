package com.moebius.entropy.dto.order;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.DummyOrderConfig;
import lombok.*;

@Getter
@RequiredArgsConstructor
public class DividedDummyOrderDto {
	private final Market market;
	private final InflationConfig inflationConfig;
	private final DummyOrderConfig askOrderConfig;
	private final DummyOrderConfig bidOrderConfig;
}
