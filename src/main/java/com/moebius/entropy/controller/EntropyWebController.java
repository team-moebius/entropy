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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.Duration;
import java.util.Map;

@SuppressWarnings("unused")
@RequiredArgsConstructor
@Slf4j
@Controller
@RequestMapping("/")
public class EntropyWebController {

	private final static Market GTAX_MARKET = new Market(Exchange.BOBOO, "GTAX2USDT", TradeCurrency.USDT, 2, 2);
	private final static Map<String, Market> MARKETS = Map.of(
		"gtax2", GTAX_MARKET,
		"ethv", new Market(Exchange.BOBOO, "ETHVUSDT", TradeCurrency.USDT, 2, 1),
		"moi", new Market(Exchange.BOBOO, "MOIUSDT", TradeCurrency.USDT, 2, 2));
	private final EntropyViewService viewService;

	@GetMapping(value = "")
    public String index(Model model) {
        model.addAttribute("market", "gtax2");
        return "index";
    }

	@GetMapping(value = "/{market}")
	public String index(@PathVariable("market") String market, Model model) {
		if (!MARKETS.containsKey(market.toLowerCase())) {
			model.addAttribute("market", "gtax2");
		} else {
			model.addAttribute("market", market);
		}

		return "index";
	}

	@GetMapping(value = "/{market}/subscribe-market-prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@ResponseBody
	public Flux<ServerSentEvent<MarketPriceDto>> getPriceStream(@PathVariable("market") String market) {
		return viewService.receiveMarketPriceDto(MARKETS.getOrDefault(market.toLowerCase(), GTAX_MARKET), Duration.ofSeconds(1L))
			.map(marketPrice -> ServerSentEvent
				.builder(marketPrice)
				.build()
			);
	}

	@PostMapping("/{market}/order/automatic")
	@ResponseBody
	public Mono<AutomaticOrderResult> requestAutomaticOrder(
		@PathVariable("market") String market,
		@Valid @RequestBody AutomaticOrderForm orderForm) {
		return viewService.startAutomaticOrder(MARKETS.getOrDefault(market.toLowerCase(), GTAX_MARKET), orderForm);
	}

	@DeleteMapping("/{market}/order/automatic")
	@ResponseBody
	public Mono<AutomaticOrderCancelResult> cancelAutomaticOrder(@PathVariable("market") String market) {
		return viewService.cancelAutomaticOrder(AutomaticOrderCancelForm.builder()
			.market(MARKETS.getOrDefault(market.toLowerCase(), GTAX_MARKET))
			.build());
	}

	@PostMapping("/{market}/order/manual")
	@ResponseBody
	public Mono<ManualOrderResult> requestAutomaticOrder(
		@PathVariable("market") String market,
		@Valid @RequestBody ManualOrderForm orderForm) {
		return viewService.requestManualOrder(MARKETS.getOrDefault(market.toLowerCase(), GTAX_MARKET), orderForm);
	}
}
