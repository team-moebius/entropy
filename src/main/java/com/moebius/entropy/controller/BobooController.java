package com.moebius.entropy.controller;

import com.moebius.entropy.dto.exchange.order.ApiKeyDto;
import com.moebius.entropy.dto.exchange.order.boboo.BobooOpenOrdersDto;
import com.moebius.entropy.service.exchange.BobooService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/boboo")
@RequiredArgsConstructor
public class BobooController {
	private final BobooService bobooService;

	@GetMapping("/open-orders")
	public Flux<BobooOpenOrdersDto> getOpenOrders(@RequestParam String symbol, @RequestBody ApiKeyDto apiKeyDto) {
		return bobooService.getOpenOrders(symbol, apiKeyDto);
	}
}
