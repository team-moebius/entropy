package com.moebius.entropy.assembler;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.DummyOrderConfig;
import com.moebius.entropy.dto.MarketDto;
import com.moebius.entropy.dto.order.DividedDummyOrderDto;
import com.moebius.entropy.dto.view.AutomaticOrderForm;
import com.moebius.entropy.dto.view.AutomaticOrderResult;
import org.springframework.stereotype.Component;

@Component
public class AutomaticOrderViewAssembler {

    public InflationConfig assembleInflationConfig(AutomaticOrderForm automaticOrderForm) {

        return InflationConfig.builder()
            .askCount(Math.toIntExact(automaticOrderForm.getSellInflationCount()))
            .bidCount(Math.toIntExact(automaticOrderForm.getBuyInflationCount()))
            .askMinVolume(automaticOrderForm.getSellVolumeRangeFrom())
            .askMaxVolume(automaticOrderForm.getSellVolumeRangeTo())
            .enable(true)
            .build();
    }

    public DividedDummyOrderDto assembleDivideDummyOrder(
        Market market, AutomaticOrderForm automaticOrderForm
    ) {
        return DividedDummyOrderDto.builder()
            .market(assembleMarketDto(market))
            .inflationConfig(assembleInflationConfig(automaticOrderForm))
            .askOrderConfig(DummyOrderConfig.builder()
                .minDividedOrderCount(Math.toIntExact(automaticOrderForm.getSellDivisionTimeFrom()))
                .maxDividedOrderCount(Math.toIntExact(automaticOrderForm.getSellDivisionTimeTo()))
                .period(automaticOrderForm.getSellDivisionInterval().floatValue())
                .minReorderCount(Math.toIntExact(automaticOrderForm.getSellDivisionFrom()))
                .maxReorderCount(Math.toIntExact(automaticOrderForm.getSellDivisionTo()))
                .build())
            .bidOrderConfig(DummyOrderConfig.builder()
                .minDividedOrderCount(Math.toIntExact(automaticOrderForm.getBuyDivisionTimeFrom()))
                .maxDividedOrderCount(Math.toIntExact(automaticOrderForm.getBuyDivisionTimeTo()))
                .period(automaticOrderForm.getBuyDivisionInterval().floatValue())
                .minReorderCount(Math.toIntExact(automaticOrderForm.getBuyDivisionFrom()))
                .maxReorderCount(Math.toIntExact(automaticOrderForm.getBuyDivisionTo()))
                .build())
            .build();
    }

    private MarketDto assembleMarketDto(Market market) {
        return MarketDto.builder()
            .exchange(market.getExchange())
            .symbol(market.getSymbol())
            .tradeCurrency(market.getTradeCurrency())
            .build();
    }

    public AutomaticOrderResult assembleAutomaticOrderResult(String disposableId) {
        return AutomaticOrderResult.builder()
            .disposableId(disposableId)
            .build();
    }
}
