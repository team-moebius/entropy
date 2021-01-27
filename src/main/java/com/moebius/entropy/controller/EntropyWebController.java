package com.moebius.entropy.controller;


import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeCurrency;
import com.moebius.entropy.dto.view.AutomaticOrderCancelForm;
import com.moebius.entropy.dto.view.AutomaticOrderCancelResult;
import com.moebius.entropy.dto.view.AutomaticOrderForm;
import com.moebius.entropy.dto.view.AutomaticOrderResult;
import com.moebius.entropy.dto.view.ManualOrderForm;
import com.moebius.entropy.service.view.EntropyViewService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

@SuppressWarnings("unused")
@RequiredArgsConstructor
@Slf4j
@Controller
@RequestMapping("/")
public class EntropyWebController {

    private static final Market market = new Market(Exchange.BOBOO, "GTAX", TradeCurrency.USDT);
    private final EntropyViewService viewService;

    @GetMapping("/")
    public String index() {
        return "index";
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
        @RequestParam String disposableId) {
        return viewService.cancelAutomaticOrder(AutomaticOrderCancelForm.builder()
            .disposableId(disposableId)
            .market(market)
            .build());
    }

    @PostMapping("/order/manual")
    @ResponseBody
    public ManualOrderForm requestAutomaticOrder(@Valid @RequestBody ManualOrderForm orderForm) {
        log.info(orderForm.toString());
        return orderForm;
    }
}
