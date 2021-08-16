package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.assembler.TradeWindowAssembler;
import com.moebius.entropy.assembler.TradeWindowAssemblerFactory;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflateRequest;
import com.moebius.entropy.dto.exchange.orderbook.OrderBookDto;
import com.moebius.entropy.util.SymbolUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TradeWindowChangeEventListener {
	private final TradeWindowAssemblerFactory assemblerFactory;
	private final TradeWindowCommandService commandService;
	private final TradeWindowInflateService inflateService;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void inflateOrdersOnTradeWindowChange(OrderBookDto orderBookDto) {
	    Market market = SymbolUtil.marketFromSymbol(orderBookDto.getSymbol());
        TradeWindowAssembler<?> assembler = Optional.ofNullable(market)
			.map(foundMarket -> assemblerFactory.getTradeWindowAssembler(foundMarket.getExchange()))
			.orElse(null);

		Optional.ofNullable(assembler)
			.map(tradeWindowAssembler -> tradeWindowAssembler.assembleTradeWindow(orderBookDto))
			.map(tradeWindow -> {
				BigDecimal marketPrice = assembler.extractMarketPrice(orderBookDto);

				commandService.saveCurrentTradeWindow(market, marketPrice, tradeWindow);
				return market;
			})
			.ifPresent(checkedMarket -> {
				InflateRequest request = new InflateRequest(checkedMarket);
				inflateService.inflateOrders(request).subscribe();
			});
	}
}
