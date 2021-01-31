package com.moebius.entropy.domain.order;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RepeatMarketOrderConfig {
	private BigDecimal minVolume;
	private BigDecimal maxVolume;
	private float period;
	private int minReorderCount;
	private int maxReorderCount;
}
