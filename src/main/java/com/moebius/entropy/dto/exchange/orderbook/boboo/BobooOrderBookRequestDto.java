package com.moebius.entropy.dto.exchange.orderbook.boboo;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class BobooOrderBookRequestDto {
	private String symbol;
	private String topic;
	private String event;
	private Map<String, String> params;
}
