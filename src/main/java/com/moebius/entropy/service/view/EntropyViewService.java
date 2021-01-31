package com.moebius.entropy.service.view;

import com.moebius.entropy.assembler.AutomaticOrderViewAssembler;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.dto.view.AutomaticOrderCancelForm;
import com.moebius.entropy.dto.view.AutomaticOrderCancelResult;
import com.moebius.entropy.dto.view.AutomaticOrderForm;
import com.moebius.entropy.dto.view.AutomaticOrderResult;
import com.moebius.entropy.repository.InflationConfigRepository;
import com.moebius.entropy.service.order.boboo.BobooDividedDummyOrderService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class EntropyViewService {

    private final AutomaticOrderViewAssembler automaticOrderViewAssembler;
    private final BobooDividedDummyOrderService dividedDummyOrderService;
    private final InflationConfigRepository inflationConfigRepository;

    public Mono<AutomaticOrderResult> startAutomaticOrder(Market market,
        AutomaticOrderForm automaticOrderForm) {
        var inflationConfig = automaticOrderViewAssembler
            .assembleInflationConfig(automaticOrderForm);

        inflationConfigRepository.saveConfigFor(market, inflationConfig);

        return Mono.just(automaticOrderForm)
            .map(form -> automaticOrderViewAssembler.assembleDivideDummyOrder(market, form))
            .flatMap(dividedDummyOrderService::executeDividedDummyOrders)
            .map(HttpEntity::getBody)
            .map(String::valueOf)
            .map(automaticOrderViewAssembler::assembleAutomaticOrderResult);
    }

    public Mono<AutomaticOrderCancelResult> cancelAutomaticOrder(
        AutomaticOrderCancelForm cancelForm) {
        var market = cancelForm.getMarket();
        boolean inflationCancelled = Objects.nonNull(market);

        if (inflationCancelled) {
            var inflationConfig = inflationConfigRepository.getConfigFor(market);
            InflationConfig disabledConfig = inflationConfig.disable();
            inflationConfigRepository.saveConfigFor(market, disabledConfig);
        }

        String disposableId = cancelForm.getDisposableId();
        return Mono.just(disposableId)
            .filter(StringUtils::isNotEmpty)
            .flatMap(dividedDummyOrderService::stopDividedDummyOrders)
            .map(ResponseEntity::getBody)
            .map(Object::toString)
            .filter(disposableId::equals)
            .map(cancelledId -> AutomaticOrderCancelResult.builder()
                .cancelledDisposableId(cancelledId)
                .inflationCancelled(inflationCancelled)
                .build())
            .switchIfEmpty(Mono.just(AutomaticOrderCancelResult.builder()
                .inflationCancelled(inflationCancelled)
                .build()));
    }
}