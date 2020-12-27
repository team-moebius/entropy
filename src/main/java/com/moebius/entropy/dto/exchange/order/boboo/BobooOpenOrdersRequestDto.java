package com.moebius.entropy.dto.exchange.order.boboo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BobooOpenOrdersRequestDto {
	private String signature;
	private long timestamp;
}
