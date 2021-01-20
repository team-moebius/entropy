package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.TradeWindowRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeWindowCommandService {

    private final TradeWindowRepository tradeWindowRepository;

    public void saveCurrentTradeWindow(Market market, BigDecimal marketPrice,
        TradeWindow tradeWindow) {
        tradeWindowRepository.savePriceForSymbol(market, marketPrice);
        tradeWindowRepository.saveTradeWindowForSymbol(market, tradeWindow);
    }
}
