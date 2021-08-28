package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.TradeDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeWindowCommandService {
	private final TradeDataRepository tradeDataRepository;
	private final TradeDataServiceFactory tradeDataServiceFactory;

	public void saveCurrentTradeWindow(Market market, BigDecimal marketPrice,
		TradeWindow tradeWindow) {
		tradeDataRepository.saveMarketPrice(market, marketPrice);

		if (tradeWindow == null ||
			(CollectionUtils.isEmpty(tradeWindow.getAskPrices()) && CollectionUtils.isEmpty(tradeWindow.getBidPrices()))) {
			log.warn("[{}-{}] There is an empty trade window, please check the inflation.", market.getExchange(), market.getSymbol());
			return;
		}

		Optional.ofNullable(tradeDataServiceFactory.getTradeDataService(market.getExchange()))
			.ifPresent(tradeDataService -> tradeDataService.handleTradeWindow(market, tradeWindow));
	}
}
