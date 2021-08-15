package com.moebius.entropy.assembler.boboo;

import com.moebius.entropy.assembler.TradeWindowAssembler;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.dto.exchange.orderbook.OrderBookDto;
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public final class BobooTradeWindowAssembler extends TradeWindowAssembler<Data> {
	@Override
	public BigDecimal extractMarketPrice(OrderBookDto<Data> orderBookDto) {
		return Optional.ofNullable(orderBookDto)
			.map(OrderBookDto::getData)
			.map(this::findFirst)
			.map(Data::getAsks)
			.map(this::findFirst)
			.map(pair -> new BigDecimal(pair.get(0)))
			.orElseThrow(() -> new IllegalStateException(
				String.format(
					"[%s] Failed to extract market price from BobooOrderBookDto due to data missing %s",
					getClass().getName(), orderBookDto
				)
			));
	}

	@Override
	public Exchange getExchange() {
		return Exchange.BOBOO;
	}

	@Override
	protected List<TradePrice> mapTrade(OrderPosition orderPosition, Data data) {
		List<List<String>> rawTradePrices = null;

		if (orderPosition == OrderPosition.ASK) {
			rawTradePrices = data.getAsks();
		} else if (orderPosition == OrderPosition.BID) {
			rawTradePrices = data.getBids();
		}

		return Optional.ofNullable(rawTradePrices)
			.map(prices -> prices.stream().map(pair -> {
				BigDecimal unitPrice = new BigDecimal(pair.get(0));
				BigDecimal volume = new BigDecimal(pair.get(1));
				return new TradePrice(orderPosition, unitPrice, volume);

			}).collect(Collectors.toList()))
			.orElse(Collections.emptyList());
	}
}
