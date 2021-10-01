package com.moebius.entropy.service.tradewindow.bigone;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.TradeDataRepository;
import com.moebius.entropy.service.tradewindow.TradeDataService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BigoneTradeDataService implements TradeDataService {
	private final static int TRADE_UPDATE_COUNT_STANDARD = 10;

	private final TradeDataRepository tradeDataRepository;

	@Override
	public void handleTradeWindow(Market market, TradeWindow tradeWindow) {
		TradeWindow oldWindow = tradeDataRepository.getTradeWindowByMarket(market);

		if (needUpdateTradeWindow(oldWindow, tradeWindow)) {
			tradeDataRepository.saveTradeWindow(market, updateTradeWindow(oldWindow, tradeWindow));
		} else {
			tradeDataRepository.saveTradeWindow(market, tradeWindow);
		}
	}

	@Override
	public Exchange getExchange() {
		return Exchange.BIGONE;
	}

	private boolean needUpdateTradeWindow(TradeWindow oldWindow, TradeWindow newWindow) {
		int oldAskSize = Optional.ofNullable(oldWindow)
			.map(TradeWindow::getAskPrices)
			.map(List::size)
			.orElse(0);
		int oldBidSize = Optional.ofNullable(oldWindow)
			.map(TradeWindow::getBidPrices)
			.map(List::size)
			.orElse(0);

		return oldAskSize > TRADE_UPDATE_COUNT_STANDARD && newWindow.getAskPrices().size() < TRADE_UPDATE_COUNT_STANDARD
			&& oldBidSize > TRADE_UPDATE_COUNT_STANDARD && newWindow.getBidPrices().size() < TRADE_UPDATE_COUNT_STANDARD;
	}

	private TradeWindow updateTradeWindow(TradeWindow oldWindow, TradeWindow updatedWindow) {
		List<TradePrice> updatedAskPrices = updatedWindow.getAskPrices();
		List<TradePrice> updatedBidPrices = updatedWindow.getBidPrices();

		List<TradePrice> oldAskPrices = oldWindow.getAskPrices();
		List<TradePrice> oldBidPrices = oldWindow.getBidPrices();

		oldAskPrices.removeIf(tradePrice -> updatedAskPrices.stream()
			.filter(Objects::nonNull)
			.anyMatch(newTradePrice -> tradePrice.getUnitPrice().compareTo(newTradePrice.getUnitPrice()) == 0));

		oldBidPrices.removeIf(tradePrice -> updatedBidPrices.stream()
			.filter(Objects::nonNull)
			.anyMatch(newTradePrice -> tradePrice.getUnitPrice().compareTo(newTradePrice.getUnitPrice()) == 0));

		updatedAskPrices.stream()
			.filter(newTradePrice -> newTradePrice.getVolume().compareTo(BigDecimal.ZERO) > 0)
			.forEach(oldAskPrices::add);

		updatedBidPrices.stream()
			.filter(newTradePrice -> newTradePrice.getVolume().compareTo(BigDecimal.ZERO) > 0)
			.forEach(oldBidPrices::add);

		return oldWindow;
	}
}
