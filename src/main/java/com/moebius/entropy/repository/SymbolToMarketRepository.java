package com.moebius.entropy.repository;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.Symbol;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SymbolToMarketRepository {
	private final Map<Exchange, Map<Symbol, Market>> symbolToMarkets = new ConcurrentHashMap<>();

	public Map<Symbol, Market> saveSymbolToMarket(Exchange exchange, Symbol symbol, Market market) {
		Map<Symbol, Market> savedSymbolToMarkets = symbolToMarkets.getOrDefault(exchange, new ConcurrentHashMap<>());
		savedSymbolToMarkets.put(symbol, market);
		return symbolToMarkets.put(exchange, savedSymbolToMarkets);
	}

	public Map<Symbol, Market> getSymbolToMarkets(Exchange exchange) {
		return symbolToMarkets.getOrDefault(exchange, null);
	}

	public Market getMarket(Exchange exchange, Symbol symbol) {
		return Optional.ofNullable(symbolToMarkets.getOrDefault(exchange, null))
			.map(symbolToMarkets -> symbolToMarkets.getOrDefault(symbol, null))
			.orElse(null);
	}
}
