package com.moebius.entropy.domain;

import com.moebius.entropy.domain.trade.TradeCurrency;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum Symbol {
	GTAX2USDT("gtax2", new Market(Exchange.BOBOO, "GTAX2USDT", TradeCurrency.USDT, 2, 2)),
	MOIUSDT("moi", new Market(Exchange.BOBOO, "MOIUSDT", TradeCurrency.USDT, 2, 2)),
	OAUSDT("oa", new Market(Exchange.BIGONE, "OAUSDT", TradeCurrency.USDT, 2, 2));

	private final String key;
	private final Market market;

	public static Map<String, Market> getKeyToMarkets() {
		return Arrays.stream(Symbol.values())
			.collect(Collectors.toMap(Symbol::getKey, Symbol::getMarket));
	}

	public static Map<String, Market> getNameToMarkets() {
		return Arrays.stream(Symbol.values())
			.collect(Collectors.toMap(Symbol::name, Symbol::getMarket));
	}
}
