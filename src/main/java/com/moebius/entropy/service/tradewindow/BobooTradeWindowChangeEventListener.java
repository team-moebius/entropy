package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.assembler.TradeWindowAssembler;
import com.moebius.entropy.domain.InflateRequest;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BobooTradeWindowChangeEventListener {

    private final TradeWindowCommandService commandService;
    private final TradeWindowAssembler assembler;
    private final TradeWindowInflateService tradeWindowInflateService;

    public void onTradeWindowChange(BobooOrderBookDto orderBookDto) {
        Optional.ofNullable(assembler.assembleTradeWindow(orderBookDto))
            .map(tradeWindow -> {
                Market market = assembler.extractMarket(orderBookDto);
                BigDecimal marketPrice = assembler.extractMarketPrice(orderBookDto);

                commandService.saveCurrentTradeWindow(market, marketPrice, tradeWindow);
                return market;
            })
            .ifPresent(market -> {
                InflateRequest inflateRequest = new InflateRequest(market);
                tradeWindowInflateService.inflateTrades(inflateRequest);
            });
    }

}
