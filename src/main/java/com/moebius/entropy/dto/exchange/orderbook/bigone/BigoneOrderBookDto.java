package com.moebius.entropy.dto.exchange.orderbook.bigone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moebius.entropy.dto.exchange.orderbook.OrderBookDto;
import lombok.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BigoneOrderBookDto implements OrderBookDto<BigoneOrderBookDto.Depth> {
	private String requestId;
	private DepthWrapper depthSnapshot;
	private DepthWrapper depthUpdate;

	@Override
	public String getSymbol() {
		return Optional.ofNullable(getValidDepthWrapper())
			.map(DepthWrapper::getDepth)
			.map(Depth::getSymbol)
			.orElse(null);
	}

	@Override
	public List<Depth> getData() {
		return Optional.ofNullable(getValidDepthWrapper())
			.map(DepthWrapper::getDepth)
			.map(Collections::singletonList)
			.orElse(Collections.emptyList());
	}

	@Getter
	@Builder
	@ToString
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	private static class DepthWrapper {
		private Depth depth;
	}

	@Getter
	@Builder
	@ToString
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Depth {
		private String changeId;
		@JsonProperty("market")
		private String symbol;
		private List<Data> asks;
		private List<Data> bids;
	}

	@Getter
	@Builder
	@ToString
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Data {
		private BigDecimal price;
		private BigDecimal amount;
		private BigDecimal orderCount;
	}

	private DepthWrapper getValidDepthWrapper() {
		return depthSnapshot == null ? depthUpdate : depthSnapshot;
	}
}
