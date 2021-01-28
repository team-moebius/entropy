package com.moebius.entropy.dto.order;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class RepeatMarketOrderResponseDto {
	private String askOrderDisposableId;
	private String bidOrderDisposableId;
}
