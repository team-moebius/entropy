package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.assembler.TradeWindowAssembler;
import com.moebius.entropy.assembler.TradeWindowAssemblerFactory;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflateRequest;
import com.moebius.entropy.dto.exchange.orderbook.OrderBookDto;
import com.moebius.entropy.repository.InflationLockRepository;
import com.moebius.entropy.util.SymbolUtil;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeWindowChangeEventListener {

	private final static String LOCK_KEY_FORMAT = "%s-%s";

	private final TradeWindowCommandService commandService;
	private final TradeWindowInflateService inflateService;
	private final TradeWindowAssemblerFactory assemblerFactory;
	private final InflationLockRepository inflationLockRepository;

	@SuppressWarnings({"rawtypes", "unchecked"})
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

				if (!inflationLockRepository.getAndSetLock(
					String.format(LOCK_KEY_FORMAT, market.getExchange(), market.getSymbol()))) {
					return null;
				}
				return market;
			})
			.ifPresent(checkedMarket -> {
				InflateRequest request = new InflateRequest(checkedMarket);
				inflateService.inflateOrders(request)
					.doOnTerminate(() -> inflationLockRepository.unsetLock(String
						.format(LOCK_KEY_FORMAT, checkedMarket.getExchange(),
							checkedMarket.getSymbol())))
					.subscribe();
			});
	}
}
