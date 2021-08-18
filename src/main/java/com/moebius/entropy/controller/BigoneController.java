package com.moebius.entropy.controller;

import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneOpenOrderDto;
import com.moebius.entropy.dto.order.DividedDummyOrderDto;
import com.moebius.entropy.dto.order.RepeatMarketOrderDto;
import com.moebius.entropy.service.exchange.bigone.BigoneExchangeService;
import com.moebius.entropy.service.order.auto.DividedDummyOrderService;
import com.moebius.entropy.service.order.auto.RepeatMarketOrderService;
import com.moebius.entropy.service.order.bigone.BigoneOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bigone")
@RequiredArgsConstructor
@Slf4j
public class BigoneController {
	private final BigoneExchangeService bigoneExchangeService;
	private final DividedDummyOrderService dividedDummyOrderService;
	private final RepeatMarketOrderService repeatMarketOrderService;
	private final BigoneOrderService bigoneOrderService;

	@PostMapping("/open-orders")
	public Flux<BigoneOpenOrderDto> getOpenOrders(@RequestParam String symbol, @RequestBody ApiKey apiKeyDto) {
		return bigoneExchangeService.getOpenOrders(symbol, apiKeyDto);
	}

	@PostMapping("/divided-dummy-order")
	public Mono<ResponseEntity<?>> testDividedDummyOrder(@RequestBody DividedDummyOrderDto dividedDummyOrderDto) {
		return dividedDummyOrderService.executeDividedDummyOrders(dividedDummyOrderDto);
	}

	@PostMapping("/repeat-market-order")
	public Mono<ResponseEntity<?>> testRepeatMarketOrder(@RequestBody RepeatMarketOrderDto repeatMarketOrderDto) {
		return repeatMarketOrderService.executeRepeatMarketOrders(repeatMarketOrderDto);
	}

	@DeleteMapping("/order")
	public Mono<ResponseEntity<?>> testStopRepeatMarketOrder(@RequestParam String disposableId) {
		return bigoneOrderService.stopOrder(disposableId);
	}
}
