package com.moebius.entropy.assembler;

import com.moebius.entropy.domain.Exchange;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TradeWindowAssemblerFactory {
	private final Map<Exchange, TradeWindowAssembler<?>> tradeWindowAssemblerMap;

	public TradeWindowAssemblerFactory(List<TradeWindowAssembler<?>> tradeWindowAssemblers) {
		this.tradeWindowAssemblerMap = tradeWindowAssemblers.stream()
			.collect(Collectors.toMap(TradeWindowAssembler::getExchange, tradeWindowAssembler -> tradeWindowAssembler));
	}

	public TradeWindowAssembler<?> getTradeWindowAssembler(Exchange exchange) {
		return tradeWindowAssemblerMap.getOrDefault(exchange, null);
	}
}
