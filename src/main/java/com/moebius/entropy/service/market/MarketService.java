package com.moebius.entropy.service.market;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.Symbol;
import com.moebius.entropy.repository.MarketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketRepository marketRepository;

    public Market findMarket(String exchangeName, String symbolName) {
        if (StringUtils.isEmpty(exchangeName) || StringUtils.isEmpty(symbolName)) {
            log.warn("There is no target exchange or symbol name, return null market ... [{}-{}]",
                exchangeName, symbolName);
            return null;
        }

        Exchange exchange = Exchange.valueOf(exchangeName.toUpperCase());
        Symbol symbol = Symbol.valueOf(symbolName.toUpperCase());

        return marketRepository.getMarket(exchange, symbol);
    }
}
