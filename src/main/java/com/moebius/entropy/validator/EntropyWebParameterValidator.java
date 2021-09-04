package com.moebius.entropy.validator;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Symbol;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EntropyWebParameterValidator {

    private final Set<Exchange> validExchanges;
    private final Map<Exchange, Set<Symbol>> validSymbols;

    public EntropyWebParameterValidator(Map<Exchange, List<Symbol>> symbols) {
        validExchanges = symbols.keySet();
        validSymbols = symbols.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new HashSet<>(e.getValue())));
    }

    public boolean validateExchangeAndSymbol(String exchangeName, String symbolName) {
        try {
            Exchange exchange = Exchange.valueOf(exchangeName);
            Symbol symbol = Symbol.valueOf(symbolName);
            return validExchanges.contains(exchange)
                && Optional.ofNullable(validSymbols.getOrDefault(exchange, null))
                .map(symbols -> symbols.contains(symbol))
                .orElse(false);
        } catch (Exception e) {
            log.warn("Entered invalid parameter.", e);
            return false;
        }
    }
}
