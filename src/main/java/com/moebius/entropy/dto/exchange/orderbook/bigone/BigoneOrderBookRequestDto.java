package com.moebius.entropy.dto.exchange.orderbook.bigone;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BigoneOrderBookRequestDto {
	private String requestId;
	private DepthRequest subscribeMarketDepthRequest;

	@Getter
	@Builder
	public static class DepthRequest {
		private String market;
	}
}
