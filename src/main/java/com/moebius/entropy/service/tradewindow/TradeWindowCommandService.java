package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.TradeWindowRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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

        tradeWindowRepository.saveTradeWindowForSymbol(market, tradeWindow);
    }
}
