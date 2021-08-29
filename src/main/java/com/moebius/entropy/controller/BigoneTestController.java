package com.moebius.entropy.controller;

import com.moebius.entropy.domain.Symbol;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.dto.MarketDto;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneOpenOrderDto;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneOrderRequestDto;
import com.moebius.entropy.dto.order.DividedDummyOrderDto;
import com.moebius.entropy.dto.order.RepeatMarketOrderDto;
import com.moebius.entropy.service.exchange.bigone.BigoneExchangeService;
import com.moebius.entropy.service.order.auto.DividedDummyOrderService;
import com.moebius.entropy.service.order.auto.RepeatMarketOrderService;
import com.moebius.entropy.service.order.bigone.BigoneOrderService;
import com.moebius.entropy.util.SymbolUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bigone")
@RequiredArgsConstructor
public class BigoneTestController {
	private final DividedDummyOrderService dividedDummyOrderService;
	private final RepeatMarketOrderService repeatMarketOrderService;
	private final BigoneOrderService bigoneOrderService;

	@PostMapping("/open-orders")
	public Flux<Order> getOpenOrders(@RequestBody MarketDto marketDto) {
		return bigoneOrderService.fetchAllOrdersFor(marketDto.toDomainEntity());
	}

	@PostMapping("/divided-dummy-order")
	public Mono<ResponseEntity<?>> requestDividedDummyOrder(@RequestBody DividedDummyOrderDto dividedDummyOrderDto) {
		return dividedDummyOrderService.executeDividedDummyOrders(dividedDummyOrderDto);
	}

	@PostMapping("/repeat-market-order")
	public Mono<ResponseEntity<?>> requestRepeatMarketOrder(@RequestBody RepeatMarketOrderDto repeatMarketOrderDto) {
		return repeatMarketOrderService.executeRepeatMarketOrders(repeatMarketOrderDto);
	}

	@PostMapping("/order")
	public Mono<ResponseEntity<?>> requestOrder(@RequestBody BigoneOrderRequestDto bigoneOrderRequestDto) {
		return bigoneOrderService.requestOrder(OrderRequest.builder()
			.market(SymbolUtil.marketFromSymbol(bigoneOrderRequestDto.getSymbol()))
			.orderPosition(bigoneOrderRequestDto.getSide())
			.price(bigoneOrderRequestDto.getPrice())
			.volume(bigoneOrderRequestDto.getAmount())
			.build())
			.map(ResponseEntity::ok);
	}

	@DeleteMapping("/orders/{id}")
	public Mono<ResponseEntity<?>> cancelOrder(@PathVariable("id") String orderId) {
		return bigoneOrderService.cancelOrder(new Order(orderId, Symbol.OAUSDT.getMarket(), null, null, null))
			.map(ResponseEntity::ok);
	}

	@DeleteMapping("/auto-order")
	public Mono<ResponseEntity<?>> stopAutoOrder(@RequestParam String disposableId) {
		return bigoneOrderService.stopOrder(disposableId);
	}
}
