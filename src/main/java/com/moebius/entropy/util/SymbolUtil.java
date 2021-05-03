package com.moebius.entropy.util;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.Symbol;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;

public class SymbolUtil {
	private final static Map<String, Market> MARKETS = Symbol.getNameToMarkets();

	public static Market marketFromSymbol(String symbol) {
		return Optional.ofNullable(symbol)
			.filter(StringUtils::isNotEmpty)
			.map(MARKETS::get)
			.orElse(null);
	}

	public static String stripCurrencyFromSymbol(Market market) {
		return Symbol.valueOf(market.getSymbol()).getKey().toUpperCase();
	}
}
