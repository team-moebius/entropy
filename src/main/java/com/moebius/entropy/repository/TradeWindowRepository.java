package com.moebius.entropy.repository;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeWindow;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Repository
public class TradeWindowRepository {

	private final Map<String, BigDecimal> marketPriceForSymbol;
	private final Map<String, TradeWindow> tradeWindowForSymbol;

	public TradeWindowRepository() {
		marketPriceForSymbol = new HashMap<>();
		tradeWindowForSymbol = new HashMap<>();
	}

	public void savePriceForSymbol(Market market, BigDecimal marketPrice) {
		marketPriceForSymbol.put(keyFrom(market), marketPrice);
	}

	public BigDecimal getMarketPriceForSymbol(Market market) {
		return marketPriceForSymbol.getOrDefault(keyFrom(market), BigDecimal.ZERO);
	}

	public void saveTradeWindowForSymbol(Market market, TradeWindow tradeWindow) {
		tradeWindowForSymbol.put(keyFrom(market), tradeWindow);
	}

	public TradeWindow getTradeWindowForSymbol(Market market) {
		return tradeWindowForSymbol.getOrDefault(keyFrom(market), TradeWindow.emptyWindow);
	}

	private String keyFrom(Market market) {
		return keyFrom(market.getExchange(), market.getSymbol() + market.getTradeCurrency().name());
	}

	private String keyFrom(Exchange exchange, String symbol) {
		return String.format("%s-%s", exchange.name(), symbol);
	}
}
