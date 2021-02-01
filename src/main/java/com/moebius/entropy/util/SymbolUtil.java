package com.moebius.entropy.util;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeCurrency;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class SymbolUtil {

    public static Market marketFromSymbol(String symbol) {
        return Optional.ofNullable(symbol)
            .filter(StringUtils::isNotEmpty)
            .map(orgSymbol -> {
                TradeCurrency currency = orgSymbol.endsWith("USDT")
                    ? TradeCurrency.USDT : TradeCurrency.KRW;
                return new Market(Exchange.BOBOO, orgSymbol, currency);
            })
            .orElse(null);
    }

    public static String stripCurrencyFromSymbol(Market market) {
        String symbol = market.getSymbol();
        String currency = market.getTradeCurrency().name();
        if (symbol.endsWith(currency)) {
            return symbol.replace(currency, "");
        } else {
            return symbol;
        }
    }
}
