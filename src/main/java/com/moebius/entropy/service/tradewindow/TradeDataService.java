package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeWindow;

public interface TradeDataService {
	void handleTradeWindow(Market market, TradeWindow tradeWindow);

	Exchange getExchange();
}
