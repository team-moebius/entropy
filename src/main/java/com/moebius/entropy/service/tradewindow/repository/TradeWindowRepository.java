package com.moebius.entropy.service.tradewindow.repository;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.TradeWindow;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TradeWindowRepository {

    private final Map<String, BigDecimal> marketPriceForSymbol;
    private final Map<String, TradeWindow> tradeWindowForSymbol;

    public TradeWindowRepository() {
        marketPriceForSymbol = new HashMap<>();
        tradeWindowForSymbol = new HashMap<>();
    }

    public void savePriceForSymbol(Exchange exchange, String symbol, BigDecimal marketPrice) {
        marketPriceForSymbol.put(keyFrom(exchange, symbol), marketPrice);
    }

    public BigDecimal getMarketPriceForSymbol(Market market) {
        return marketPriceForSymbol.getOrDefault(keyFrom(market), BigDecimal.ZERO);
    }

    public void saveTradeWindowForSymbol(Exchange exchange, String symbol,
        TradeWindow tradeWindow) {
        tradeWindowForSymbol.put(keyFrom(exchange, symbol), tradeWindow);
    }

    public TradeWindow getTradeWindowForSymbol(Market market) {
        return tradeWindowForSymbol.getOrDefault(keyFrom(market), TradeWindow.emptyWindow);
    }

    private String keyFrom(Market market) {
        return keyFrom(market.getExchange(), market.getSymbol());
    }

    private String keyFrom(Exchange exchange, String symbol) {
        return String.format("%s-%s", exchange.name(), symbol);
    }
}
