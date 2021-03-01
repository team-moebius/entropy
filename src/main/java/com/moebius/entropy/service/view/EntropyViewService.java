package com.moebius.entropy.service.view;

import com.moebius.entropy.assembler.AutomaticOrderViewAssembler;
import com.moebius.entropy.assembler.ManualOrderRequestAssembler;
import com.moebius.entropy.domain.ManualOrderResult;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.dto.view.AutomaticOrderCancelForm;
import com.moebius.entropy.dto.view.AutomaticOrderCancelResult;
import com.moebius.entropy.dto.view.AutomaticOrderForm;
import com.moebius.entropy.dto.view.AutomaticOrderResult;
import com.moebius.entropy.dto.view.ManualOrderForm;
import com.moebius.entropy.dto.view.MarketPriceDto;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.repository.InflationConfigRepository;
import com.moebius.entropy.service.order.boboo.BobooDividedDummyOrderService;
import com.moebius.entropy.service.order.boboo.BobooOrderService;
import com.moebius.entropy.service.order.boboo.BobooRepeatMarketOrderService;
import com.moebius.entropy.service.trade.manual.ManualOrderMakerService;
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService;
import com.moebius.entropy.util.SymbolUtil;
import java.time.Duration;
import java.util.Objects;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class EntropyViewService {

    private final AutomaticOrderViewAssembler automaticOrderViewAssembler;
    private final BobooDividedDummyOrderService dividedDummyOrderService;
    private final BobooRepeatMarketOrderService repeatMarketOrderService;
    private final InflationConfigRepository inflationConfigRepository;
    private final BobooOrderService bobooOrderService;
    private final ManualOrderRequestAssembler manualOrderRequestAssembler;
    private final ManualOrderMakerService manualOrderMakerService;
    private final DisposableOrderRepository disposableOrderRepository;
    private final TradeWindowQueryService tradeWindowQueryService;

    public Mono<AutomaticOrderResult> startAutomaticOrder(Market market,
        @Valid AutomaticOrderForm automaticOrderForm) {
        var inflationConfig = automaticOrderViewAssembler
            .assembleInflationConfig(automaticOrderForm);

        inflationConfigRepository.saveConfigFor(market, inflationConfig);

        return Mono.zip(
            Mono.just(automaticOrderViewAssembler.assembleDivideDummyOrder(market, automaticOrderForm)),
            Mono.just(automaticOrderViewAssembler.assembleRepeatMarketOrder(market, automaticOrderForm)))
            .flatMapMany(tuple -> Flux.merge(dividedDummyOrderService.executeDividedDummyOrders(tuple.getT1())
                .map(HttpEntity::getBody)
                .map(String::valueOf),
            repeatMarketOrderService.executeRepeatMarketOrders(tuple.getT2())
                .map(HttpEntity::getBody)
                .map(String::valueOf)))
            .collectList()
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

        return Flux.fromIterable(disposableOrderRepository.getAll())
            .filter(StringUtils::isNotEmpty)
            .flatMap(bobooOrderService::stopOrder)
            .map(ResponseEntity::getBody)
            .map(Object::toString)
            .collectList()
            .map(cancelledIds -> AutomaticOrderCancelResult.builder()
                .cancelledDisposableIds(cancelledIds)
                .inflationCancelled(inflationCancelled)
                .build())
            .switchIfEmpty(Mono.just(AutomaticOrderCancelResult.builder()
                .inflationCancelled(inflationCancelled)
                .build()));
    }

    public Mono<ManualOrderResult> requestManualOrder(Market market,
        @Valid ManualOrderForm manualOrderForm) {
        return Mono.just(manualOrderForm)
            .map(form -> manualOrderRequestAssembler.assembleManualOrderRequest(market, form))
            .flatMap(manualOrderMakerService::requestManualOrderMaking);
    }

    public Flux<MarketPriceDto> receiveMarketPriceDto(Market market, Duration interval) {
        return Flux.interval(interval)
            .map(index -> tradeWindowQueryService.getMarketPrice(market))
            .map(price -> MarketPriceDto.builder()
                .price(price)
                .symbol(SymbolUtil.stripCurrencyFromSymbol(market))
                .exchange(market.getExchange())
                .tradeCurrency(market.getTradeCurrency().name())
                .priceUnit(market.getTradeCurrency().getPriceUnit())
                .build());

    }
}
