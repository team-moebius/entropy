package com.moebius.entropy.service.tradewindow.hotbit;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.TradeDataRepository;
import com.moebius.entropy.service.tradewindow.TradeDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HotbitTradeDataService implements TradeDataService {
    private final TradeDataRepository tradeDataRepository;
    /**
     * tradeWindow를 저장한다.
     * @param market
     * @param tradeWindow
     */
    @Override
    public void handleTradeWindow(Market market, TradeWindow tradeWindow) {
        tradeDataRepository.saveTradeWindow(market, tradeWindow);
    }

    @Override
    public Exchange getExchange() {
        return Exchange.HOTBIT;
    }
}
