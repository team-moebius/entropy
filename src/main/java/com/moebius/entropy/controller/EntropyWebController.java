package com.moebius.entropy.controller;


import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.ManualOrderResult;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeCurrency;
import com.moebius.entropy.dto.view.*;
import com.moebius.entropy.service.view.EntropyViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.Duration;

@SuppressWarnings("unused")
@RequiredArgsConstructor
@Slf4j
@Controller
@RequestMapping("/")
public class EntropyWebController {

    private static final Market market = new Market(Exchange.BOBOO, "GTAX2USDT", TradeCurrency.USDT);
    private final EntropyViewService viewService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping(value = "/subscribe-market-prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ServerSentEvent<MarketPriceDto>> getPriceStream() {
        return viewService.receiveMarketPriceDto(market, Duration.ofSeconds(1L))
            .map(marketPrice -> ServerSentEvent
                .builder(marketPrice)
                .build()
            );
    }

    @PostMapping("/order/automatic")
    @ResponseBody
    public Mono<AutomaticOrderResult> requestAutomaticOrder(
        @Valid @RequestBody AutomaticOrderForm orderForm) {
        return viewService.startAutomaticOrder(market, orderForm);
    }

    @DeleteMapping("/order/automatic")
    @ResponseBody
    public Mono<AutomaticOrderCancelResult> cancelAutomaticOrder(
            @RequestParam(defaultValue = "disposableId") String disposableId) {
        return viewService.cancelAutomaticOrder(AutomaticOrderCancelForm.builder()
                .disposableId(disposableId)
                .market(market)
                .build());
    }

    @PostMapping("/order/manual")
    @ResponseBody
    public Mono<ManualOrderResult> requestAutomaticOrder(
        @Valid @RequestBody ManualOrderForm orderForm) {
        return viewService.requestManualOrder(market, orderForm);
    }
}
