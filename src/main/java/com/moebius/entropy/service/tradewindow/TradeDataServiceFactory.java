package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Exchange;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class TradeDataServiceFactory {
	private final Map<Exchange, TradeDataService> tradeDataServiceMap;

	public TradeDataServiceFactory(List<TradeDataService> exchangeTradeWindowServices) {
		this.tradeDataServiceMap = exchangeTradeWindowServices.stream().collect(
			Collectors.toMap(TradeDataService::getExchange, Function.identity()));
	}

	public TradeDataService getTradeDataService(Exchange exchange) {
		return tradeDataServiceMap.getOrDefault(exchange, null);
	}
}
