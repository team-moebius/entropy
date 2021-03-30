package com.moebius.entropy.domain.order.config;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DummyOrderConfig {
	private int minDividedOrderCount;
	private int maxDividedOrderCount;
	private float period;
	private int minReorderCount;
	private int maxReorderCount;
}
