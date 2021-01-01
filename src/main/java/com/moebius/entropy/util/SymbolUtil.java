package com.moebius.entropy.util;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.TradeCurrency;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class SymbolUtil {
    private static final String mergedSymbolFormat = "%s%s";

    public static String symbolFromMarket(Market market) {
        return String.format(mergedSymbolFormat, market.getSymbol(), market.getTradeCurrency().name());
    }

    public static Market marketFromSymbol(String symbol) {
        return Optional.ofNullable(symbol)
                .filter(StringUtils::isNotEmpty)
                .map(orgSymbol -> {
                    TradeCurrency currency = orgSymbol.endsWith("USDT")
                            ? TradeCurrency.USDT : TradeCurrency.KRW;
                    String coinSymbol = StringUtils.substringBefore(orgSymbol, currency.name());
                    return new Market(Exchange.BOBOO, coinSymbol, currency);
                })
                .orElse(null);
    }
}
