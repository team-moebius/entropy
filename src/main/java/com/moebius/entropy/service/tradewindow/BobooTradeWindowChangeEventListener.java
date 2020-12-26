package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.assembler.TradeWindowAssembler;
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

    public void onTradeWindowChange(BobooOrderBookDto orderBookDto) {
        Optional.ofNullable(assembler.assembleTradeWindow(orderBookDto))
            .ifPresent(tradeWindow -> {
                Market market = assembler.extractMarket(orderBookDto);
                BigDecimal marketPrice = assembler.extractMarketPrice(orderBookDto);

                commandService.saveCurrentTradeWindow(market, marketPrice, tradeWindow);
            });
    }

}
