package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.InflationConfig;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.OrderType;
import com.moebius.entropy.service.order.OrderQuantityService;
import com.moebius.entropy.service.tradewindow.repository.InflationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TradeWindowInflationVolumeResolver {
    private final InflationConfigRepository inflationConfigRepository;
    private final OrderQuantityService orderQuantityService;
    private final static int decimalPosition = 2;

    public BigDecimal getInflationVolume(Market market, OrderType orderType) {
        return Optional.ofNullable(inflationConfigRepository.getConfigFor(market))
                .map(inflationConfig -> {
                    BigDecimal maxVolume;
                    BigDecimal minVolume;
                    if (OrderType.ASK.equals(orderType)) {
                        maxVolume = inflationConfig.getAskMaxVolume();
                        minVolume = inflationConfig.getAskMinVolume();
                    } else {
                        maxVolume = inflationConfig.getBidMaxVolume();
                        minVolume = inflationConfig.getBidMinVolume();
                    }
                    return Pair.of(minVolume, maxVolume);
                })
                .map(rangePair -> orderQuantityService.getRandomQuantity(
                        rangePair.getLeft().floatValue(), rangePair.getRight().floatValue(), decimalPosition
                ))
                .orElse(BigDecimal.ZERO);
    }
}

