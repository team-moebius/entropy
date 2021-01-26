package com.moebius.entropy.service.view;

import com.moebius.entropy.assembler.AutomaticOrderViewAssembler;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.trade.TradeCurrency;
import com.moebius.entropy.dto.view.AutomaticOrderForm;
import com.moebius.entropy.dto.view.AutomaticOrderResult;
import com.moebius.entropy.repository.InflationConfigRepository;
import com.moebius.entropy.service.order.boboo.BobooDividedDummyOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class EntropyViewService {

    private static final Market market = new Market(Exchange.BOBOO, "GTAX", TradeCurrency.USDT);
    private final AutomaticOrderViewAssembler automaticOrderViewAssembler;
    private final BobooDividedDummyOrderService dividedDummyOrderService;
    private final InflationConfigRepository inflationConfigRepository;

    public Mono<AutomaticOrderResult> startAutomaticOrder(AutomaticOrderForm automaticOrderForm) {
        InflationConfig inflationConfig = automaticOrderViewAssembler
            .assembleInflationConfig(automaticOrderForm);

        inflationConfigRepository.saveConfigFor(market, inflationConfig);

        return Mono.just(automaticOrderForm)
            .map(form -> automaticOrderViewAssembler.assembleDivideDummyOrder(market, form))
            .flatMap(dividedDummyOrderService::executeDividedDummyOrders)
            .map(HttpEntity::getBody)
            .map(String::valueOf)
            .map(automaticOrderViewAssembler::assembleAutomaticOrderResult);
    }
}
