package com.moebius.entropy.dto;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeCurrency;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class MarketDto {
	private Exchange exchange;
	private String symbol;
	private TradeCurrency tradeCurrency;
	private int decimalPosition;

	public Market toDomainEntity() {
		return new Market(exchange, symbol, tradeCurrency, decimalPosition);
	}
}