package com.moebius.entropy.dto.order;

import com.moebius.entropy.domain.order.RepeatMarketOrderConfig;
import com.moebius.entropy.dto.MarketDto;
import lombok.*;

/**
 * If we need to divide RepeatMarketOrderConfig into another dto for extension, let's change this field to dto separately.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class RepeatMarketOrderDto {
	private MarketDto market;
	private RepeatMarketOrderConfig askOrderConfig;
	private RepeatMarketOrderConfig bidOrderConfig;
}
