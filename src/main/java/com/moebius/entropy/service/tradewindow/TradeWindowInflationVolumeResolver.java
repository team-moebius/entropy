package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.OrderType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TradeWindowInflationVolumeResolver {
    public BigDecimal getInflationVolume(Market market, OrderType orderType) {
        return null;
    }
}

