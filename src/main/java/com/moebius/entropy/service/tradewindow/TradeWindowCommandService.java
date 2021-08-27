package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.TradeWindowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeWindowCommandService {

	private final TradeWindowRepository tradeWindowRepository;

	public void saveCurrentTradeWindow(Market market, BigDecimal marketPrice,
		TradeWindow tradeWindow) {
		tradeWindowRepository.savePriceForSymbol(market, marketPrice);

		if (tradeWindow == null ||
			(CollectionUtils.isEmpty(tradeWindow.getAskPrices()) && CollectionUtils.isEmpty(tradeWindow.getBidPrices()))) {
			log.warn("[{}-{}] There is an empty trade window, please check the inflation.", market.getExchange(), market.getSymbol());
			return;
		}

		TradeWindow oldWindow = tradeWindowRepository.getTradeWindowForSymbol(market);

		if (isNotValidTradeWindow(oldWindow, tradeWindow)) {
			log.info("[{}-{}] There is an invalid trade window. [{}]", market.getExchange(), market.getSymbol(), tradeWindow);
			return;
		}

		tradeWindowRepository.saveTradeWindowForSymbol(market, tradeWindow);
	}

	private boolean isNotValidTradeWindow(TradeWindow oldWindow, TradeWindow tradeWindow) {
		int oldAskSize = Optional.ofNullable(oldWindow)
			.map(TradeWindow::getAskPrices)
			.map(List::size)
			.orElse(0);
		int oldBidSize = Optional.ofNullable(oldWindow)
			.map(TradeWindow::getBidPrices)
			.map(List::size)
			.orElse(0);

		return oldAskSize / 2 > tradeWindow.getAskPrices().size()
			|| oldBidSize / 2 > tradeWindow.getBidPrices().size();
	}
}
