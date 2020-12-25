package com.moebius.entropy.dto.exchange.orderbook.boboo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BobooOrderBookDto implements OrderBookDto<BobooOrderBookDto.Data> {
	private String symbol;
	private String symbolName;
	private String topic;
	private Map<String, String> params;
	private List<BobooOrderBookDto.Data> data;

	@Override
	public String getSymbol() {
		return symbol;
	}

	@Override
	public List<BobooOrderBookDto.Data> getData() {
		return data;
	}

	@Getter
	@Builder
	@ToString
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Data {
		@JsonProperty("s")
		private String symbol;
		@JsonProperty("t")
		private String timestamp;
		@JsonProperty("v")
		private String version;
		@JsonProperty("b")
		private List<List<String>> bids;
		@JsonProperty("a")
		private List<List<String>> asks;
	}
}
