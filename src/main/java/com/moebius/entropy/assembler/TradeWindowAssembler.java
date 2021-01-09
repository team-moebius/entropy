package com.moebius.entropy.assembler;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.OrderPosition;
import com.moebius.entropy.domain.TradePrice;
import com.moebius.entropy.domain.TradeWindow;
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto;
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto.Data;
import com.moebius.entropy.util.SymbolUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TradeWindowAssembler {

    public TradeWindow assembleTradeWindow(BobooOrderBookDto bobooOrderBookDto) {
        return Optional.ofNullable(bobooOrderBookDto)
            .map(BobooOrderBookDto::getData)
            .map(this::findFirst)
            .map(data -> {
                List<TradePrice> bidTrades = mapTrade(OrderPosition.BID, data.getBids());
                List<TradePrice> askTrades = mapTrade(OrderPosition.ASK, data.getAsks());
                return new TradeWindow(askTrades, bidTrades);
            })
            .orElse(null);
    }

    private List<TradePrice> mapTrade(OrderPosition orderPosition, List<List<String>> rawTradePrices) {
        return Optional.ofNullable(rawTradePrices)
            .map(prices -> prices.stream().map(pair -> {
                BigDecimal unitPrice = new BigDecimal(pair.get(0));
                BigDecimal volume = new BigDecimal(pair.get(1));
                return new TradePrice(orderPosition, unitPrice, volume);

            }).collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    public BigDecimal extractMarketPrice(BobooOrderBookDto bobooOrderBookDto) {
        return Optional.ofNullable(bobooOrderBookDto)
            .map(BobooOrderBookDto::getData)
            .map(this::findFirst)
            .map(Data::getAsks)
            .map(this::findFirst)
            .map(pair -> new BigDecimal(pair.get(0)))
            .orElseThrow(() -> new IllegalStateException(
                String.format(
                    "[%s] Failed to extract market price from BobooOrderBootDto due to data missing %s",
                    getClass().getName(), bobooOrderBookDto
                )
            ));
    }

    public Market extractMarket(BobooOrderBookDto bobooOrderBookDto) {
        return Optional.ofNullable(bobooOrderBookDto)
            .map(BobooOrderBookDto::getData)
            .map(this::findFirst)
            .map(Data::getSymbol)
            .map(SymbolUtil::marketFromSymbol)
            .orElseThrow();
    }

    private <U> U findFirst(List<U> data) {
        return Optional.ofNullable(data)
            .filter(list -> list.size() > 0)
            .map(list -> list.get(0))
            .orElse(null);

    }
}
