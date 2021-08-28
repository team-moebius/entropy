package com.moebius.entropy.repository;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeWindow;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TradeDataRepository {

	private final Map<String, BigDecimal> symbolToMarketPrices;
	private final Map<String, TradeWindow> symbolToTradeWindows;

	public TradeDataRepository() {
		symbolToMarketPrices = new ConcurrentHashMap<>();
		symbolToTradeWindows = new ConcurrentHashMap<>();
	}

	public void saveMarketPrice(Market market, BigDecimal marketPrice) {
		symbolToMarketPrices.put(keyFrom(market), marketPrice);
	}

	public BigDecimal getMarketPriceByMarket(Market market) {
		return symbolToMarketPrices.getOrDefault(keyFrom(market), BigDecimal.ZERO);
	}

	public void saveTradeWindow(Market market, TradeWindow tradeWindow) {
		symbolToTradeWindows.put(keyFrom(market), tradeWindow);
	}

	public TradeWindow getTradeWindowByMarket(Market market) {
		return symbolToTradeWindows.getOrDefault(keyFrom(market), TradeWindow.emptyWindow);
	}

	private String keyFrom(Market market) {
		return keyFrom(market.getExchange(), market.getSymbol() + market.getTradeCurrency().name());
	}

	private String keyFrom(Exchange exchange, String symbol) {
		return String.format("%s-%s", exchange.name(), symbol);
	}
}
