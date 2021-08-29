package com.moebius.entropy.assembler;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.dto.exchange.orderbook.OrderBookDto;
import com.moebius.entropy.util.SymbolUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public abstract class TradeWindowAssembler<ITEM> {
	public TradeWindow assembleTradeWindow(OrderBookDto<ITEM> orderBookDto) {
		return Optional.ofNullable(orderBookDto)
			.map(OrderBookDto::getData)
			.map(this::findFirst)
			.map(data -> {
				List<TradePrice> askTrades = mapTrade(OrderPosition.ASK, data);
				List<TradePrice> bidTrades = mapTrade(OrderPosition.BID, data);
				return new TradeWindow(askTrades, bidTrades);
			})
			.orElse(null);
	}

	public Market extractMarket(OrderBookDto<ITEM> orderBookDto) {
		return Optional.ofNullable(orderBookDto)
			.map(OrderBookDto::getSymbol)
			.map(SymbolUtil::marketFromSymbol)
			.orElseThrow();
	}

	public abstract BigDecimal extractMarketPrice(OrderBookDto<ITEM> orderBookDto);

	public abstract Exchange getExchange();

	protected abstract List<TradePrice> mapTrade(OrderPosition orderPosition, ITEM data);

	protected <U> U findFirst(List<U> data) {
		return Optional.ofNullable(data)
			.filter(list -> list.size() > 0)
			.map(list -> list.get(0))
			.orElse(null);
	}
}
