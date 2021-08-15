package com.moebius.entropy.assembler.bigone;

import com.moebius.entropy.assembler.TradeWindowAssembler;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.dto.exchange.orderbook.OrderBookDto;
import com.moebius.entropy.dto.exchange.orderbook.bigone.BigoneOrderBookDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public final class BigoneTradeWindowAssembler extends TradeWindowAssembler<BigoneOrderBookDto.Depth> {
	@Override
	public BigDecimal extractMarketPrice(OrderBookDto<BigoneOrderBookDto.Depth> orderBookDto) {
		return Optional.ofNullable(orderBookDto)
			.map(OrderBookDto::getData)
			.map(this::findFirst)
			.map(BigoneOrderBookDto.Depth::getAsks)
			.map(this::findFirst)
			.map(BigoneOrderBookDto.Data::getPrice)
			.orElseThrow(() -> new IllegalStateException(
				String.format(
					"[%s] Failed to extract market price from BigoneOrderBookDto due to data missing %s",
					getClass().getName(), orderBookDto
				)
			));
	}

	@Override
	public Exchange getExchange() {
		return Exchange.BIGONE;
	}

	@Override
	protected List<TradePrice> mapTrade(OrderPosition orderPosition, BigoneOrderBookDto.Depth depth) {
		List<BigoneOrderBookDto.Data> rawTradeData = null;

		if (orderPosition == OrderPosition.ASK) {
			rawTradeData = depth.getAsks();
		} else if (orderPosition == OrderPosition.BID) {
			rawTradeData = depth.getBids();
		}

		return Optional.ofNullable(rawTradeData)
			.stream()
			.flatMap(Collection::stream)
			.map(data -> new TradePrice(orderPosition, data.getPrice(), data.getAmount()))
			.collect(Collectors.toList());
	}
}
