package com.moebius.entropy.controller;

import com.moebius.entropy.dto.exchange.order.ApiKeyDto;
import com.moebius.entropy.dto.exchange.order.boboo.BobooOpenOrdersDto;
import com.moebius.entropy.service.exchange.boboo.BobooExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/boboo")
@RequiredArgsConstructor
public class BobooController {
	private final BobooExchangeService bobooExchangeService;

	/**
	 * Sample request for testing
	 *
	 * GET /boboo/open-orders?symbol=GTAXUSDT HTTP/1.1
	 * Host: 127.0.0.1:8080
	 * Content-Type: application/json
	 *
	 * {
	 *     "accessKey": "ulty827N2vakJap9OdAAejTe7VwykXxUplXSbPGhkLq5r5ImeXACdcr3pZhl6inF",
	 *     "secretKey": "QCzWtRLiuB1vyd1wrmcpZ7m2HywSzasGhQv0olXvUXxpezNhn9RQwFnW2nWYX6lH"
	 * }
	 */
	@GetMapping("/open-orders")
	public Flux<BobooOpenOrdersDto> getOpenOrders(@RequestParam String symbol, @RequestBody ApiKeyDto apiKeyDto) {
		return bobooExchangeService.getOpenOrders(symbol, apiKeyDto);
	}
}
