package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.TradeWindow;
import com.moebius.entropy.service.tradewindow.repository.TradeWindowRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TradeWindowCommandService {

    private final TradeWindowRepository tradeWindowRepository;

    public void saveCurrentTradeWindow(Market market, BigDecimal marketPrice,
        TradeWindow tradeWindow) {
        tradeWindowRepository.savePriceForSymbol(market, marketPrice);
        tradeWindowRepository.saveTradeWindowForSymbol(market, tradeWindow);
    }
}
