package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.assembler.TradeWindowAssembler;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflateRequest;
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BobooTradeWindowChangeEventListener {

    private final TradeWindowCommandService commandService;
    private final TradeWindowAssembler assembler;
    private TradeWindowInflateService tradeWindowInflateService;

    public void inflateOrdersOnTradeWindowChange(BobooOrderBookDto orderBookDto) {
        Optional.ofNullable(assembler.assembleTradeWindow(orderBookDto))
            .map(tradeWindow -> {
                Market market = assembler.extractMarket(orderBookDto);
                BigDecimal marketPrice = assembler.extractMarketPrice(orderBookDto);

                commandService.saveCurrentTradeWindow(market, marketPrice, tradeWindow);
                return market;
            })
            .ifPresent(market -> {
                InflateRequest request = new InflateRequest(market);
                tradeWindowInflateService.inflateOrders(request).subscribe();
            });
    }

    public void setTradeWindowInflateService(TradeWindowInflateService tradeWindowInflateService) {
        this.tradeWindowInflateService = tradeWindowInflateService;
    }
}
