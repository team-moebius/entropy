package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.TradeWindow;
import com.moebius.entropy.service.tradewindow.repository.TradeWindowRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TradeWindowQueryService {

    private final TradeWindowRepository repository;

    public Mono<TradeWindow> fetchTradeWindow(Market market) {
        return Mono.justOrEmpty(repository.getTradeWindowForSymbol(market));
    }

    public BigDecimal getMarketPrice(Market market) {
        return repository.getMarketPriceForSymbol(market);
    }
}
