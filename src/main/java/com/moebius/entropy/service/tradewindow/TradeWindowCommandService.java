package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.TradeDataRepository;
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

	private final TradeDataRepository tradeWindowRepository;

	public void saveCurrentTradeWindow(Market market, BigDecimal marketPrice,
		TradeWindow tradeWindow) {
		tradeWindowRepository.savePriceForSymbol(market, marketPrice);

		if (tradeWindow == null ||
			(CollectionUtils.isEmpty(tradeWindow.getAskPrices()) && CollectionUtils.isEmpty(tradeWindow.getBidPrices()))) {
			log.warn("[{}-{}] There is an empty trade window, please check the inflation.", market.getExchange(), market.getSymbol());
			return;
		}

		TradeWindow toBeSavedWindow = tradeWindow;
		TradeWindow oldWindow = tradeWindowRepository.getTradeWindowForSymbol(market);

		if (needUpdateTradeWindow(oldWindow, tradeWindow)) {
			toBeSavedWindow = updateTradeWindow(oldWindow, tradeWindow);
		}

		tradeWindowRepository.saveTradeWindowForSymbol(market, toBeSavedWindow);
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

		return oldAskSize / 2 > newWindow.getAskPrices().size()
			&& oldBidSize / 2 > newWindow.getBidPrices().size();
	}

	private TradeWindow updateTradeWindow(TradeWindow oldWindow, TradeWindow updatedWindow) {
		List<TradePrice> updatedAskPrices = updatedWindow.getAskPrices();
		List<TradePrice> updatedBidPrices = updatedWindow.getBidPrices();

		List<TradePrice> oldAskPrices = oldWindow.getAskPrices();
		List<TradePrice> oldBidPrices = oldWindow.getBidPrices();

		oldAskPrices.removeIf(tradePrice -> updatedAskPrices.stream()
			.anyMatch(getSamePriceAndZeroVolumePredicate(tradePrice)));

		oldBidPrices.removeIf(tradePrice -> updatedBidPrices.stream()
			.anyMatch(getSamePriceAndZeroVolumePredicate(tradePrice)));

		updatedAskPrices.stream()
			.filter(getValidVolumePredicate())
			.forEach(oldAskPrices::add);

		updatedBidPrices.stream()
			.filter(getValidVolumePredicate())
			.forEach(oldBidPrices::add);

		return oldWindow;
	}

	private Predicate<TradePrice> getSamePriceAndZeroVolumePredicate(TradePrice tradePrice) {
		return newTradePrice -> newTradePrice.getUnitPrice().compareTo(tradePrice.getUnitPrice()) == 0
			&& newTradePrice.getVolume().compareTo(BigDecimal.ZERO) == 0;
	}

	private Predicate<TradePrice> getValidVolumePredicate() {
		return tradePrice -> tradePrice.getVolume().compareTo(BigDecimal.ZERO) > 0;
	}
}
