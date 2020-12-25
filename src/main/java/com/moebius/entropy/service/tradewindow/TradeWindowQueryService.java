package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.TradeWindow;
import java.math.BigDecimal;
import reactor.core.publisher.Mono;

public class TradeWindowQueryService {

    public Mono<TradeWindow> fetchTradeWindow(Market market) {
        return null;
    }

    public BigDecimal getMarketPrice(Market market) {
        return null;
    }
}
