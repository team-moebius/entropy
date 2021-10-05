package com.moebius.entropy.dto.exchange.orderbook.bigone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moebius.entropy.dto.exchange.orderbook.OrderBookDto;
import com.moebius.entropy.dto.exchange.orderbook.bigone.BigoneOrderBookDto.Data;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BigoneOrderBookDto implements OrderBookDto<Data> {
	private DataWrapper data;

	@Override
	public String getSymbol() {
		return data.getSymbol();
	}

	@Override
	public List<Data> getData() {
		return Collections.singletonList(Data.builder()
			.asks(data.getAsks())
			.bids(data.getBids())
			.build());
	}
	@Getter
	@Builder
	@ToString
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DataWrapper {
		@JsonProperty("asset_pair_name")
		private String symbol;
		private List<UnitData> asks;
		private List<UnitData> bids;
	}

	@Getter
	@Builder
	@ToString
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Data {

		private List<UnitData> asks;
		private List<UnitData> bids;
	}

	@Getter
	@Builder
	@ToString
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class UnitData {

		private BigDecimal price;
		private BigDecimal quantity;
		@JsonProperty("order_count")
		private BigDecimal orderCount;
	}
}
