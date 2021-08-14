package com.moebius.entropy.controller;

import com.moebius.entropy.domain.order.OrderSide;
import com.moebius.entropy.domain.order.OrderType;
import com.moebius.entropy.domain.order.TimeInForce;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.dto.exchange.order.boboo.*;
import com.moebius.entropy.dto.order.DividedDummyOrderDto;
import com.moebius.entropy.dto.order.RepeatMarketOrderDto;
import com.moebius.entropy.service.exchange.BobooExchangeService;
import com.moebius.entropy.service.order.boboo.auto.BobooDividedDummyOrderService;
import com.moebius.entropy.service.order.boboo.BobooOrderService;
import com.moebius.entropy.service.order.boboo.auto.BobooRepeatMarketOrderService;
import com.moebius.entropy.util.OrderIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/boboo")
@RequiredArgsConstructor
@Slf4j
public class BobooController {
	private final BobooExchangeService bobooExchangeService;
	private final BobooDividedDummyOrderService bobooDividedDummyOrderService;
	private final BobooRepeatMarketOrderService bobooRepeatMarketOrderService;
	private final BobooOrderService bobooOrderService;

	/**
	 * Sample request for testing
	 *
	 * POST /boboo/open-orders?symbol=GTAX2USDT HTTP/1.1
	 * Host: 127.0.0.1:8080
	 * Content-Type: application/json
	 *
	 * {
	 *     "accessKey": "ulty827N2vakJap9OdAAejTe7VwykXxUplXSbPGhkLq5r5ImeXACdcr3pZhl6inF",
	 *     "secretKey": "QCzWtRLiuB1vyd1wrmcpZ7m2HywSzasGhQv0olXvUXxpezNhn9RQwFnW2nWYX6lH"
	 * }
	 */
	@PostMapping("/open-orders")
	public Flux<BobooOpenOrdersDto> getOpenOrders(@RequestParam String symbol, @RequestBody ApiKey apiKeyDto) {
		return bobooExchangeService.getOpenOrders(symbol, apiKeyDto);
	}

	@PostMapping("/divided-dummy-order")
	public Mono<ResponseEntity<?>> testDividedDummyOrder(@RequestBody DividedDummyOrderDto dividedDummyOrderDto) {
		return bobooDividedDummyOrderService.executeDividedDummyOrders(dividedDummyOrderDto);
	}

	@PostMapping("/repeat-market-order")
	public Mono<ResponseEntity<?>> testRepeatMarketOrder(@RequestBody RepeatMarketOrderDto repeatMarketOrderDto) {
		return bobooRepeatMarketOrderService.executeRepeatMarketOrders(repeatMarketOrderDto);
	}

	@DeleteMapping("/order")
	public Mono<ResponseEntity<?>> testStopRepeatMarketOrder(@RequestParam String disposableId) {
		return bobooOrderService.stopOrder(disposableId);
	}

	@PostMapping("/cancel-test-order")
	public Flux<BobooCancelResponseDto> cancelAllOpenOrders(@RequestParam String symbol, @RequestBody ApiKey apiKeyDto) {
		return bobooExchangeService.getOpenOrders(symbol, apiKeyDto)
			.map(bobooOpenOrdersDto -> BobooCancelRequestDto.builder()
				.orderId(bobooOpenOrdersDto.getInternalId())
				.build()
			)
			.flatMap(cancelRequest -> bobooExchangeService.cancelOrder(cancelRequest, apiKeyDto))
			.doOnError(throwable -> log.error("[BuyOrder] Order cancellation failed with response of: {}",
				((WebClientResponseException.BadRequest) throwable).getResponseBodyAsString(), throwable))
			.doOnEach(signal -> Optional.ofNullable(signal.get())
				.ifPresent(response -> log.info(
					"[OrderTest] Cancel order with id :{}", response.getOrderId()
					)
				));
	}

	@PostMapping("/test-buy-order")
	public Mono<BobooOrderResponseDto> testBuyOrder(@RequestParam String symbol, @RequestBody ApiKey apiKeyDto) {
		var clientOrderId = OrderIdUtil.generateOrderId();
		var orderRequest = BobooOrderRequestDto.builder()
			.symbol(symbol)
			.quantity(BigDecimal.valueOf(0.1))
			.side(OrderSide.BUY)
			.type(OrderType.LIMIT)
			.timeInForce(TimeInForce.GTC)
			.price(BigDecimal.valueOf(13.50))
			.newClientOrderId(clientOrderId)
			.build();
		return getBobooOrderResponseDto(symbol, apiKeyDto, clientOrderId, orderRequest);
	}

	@PostMapping("/test-sell-order")
	public Mono<BobooOrderResponseDto> testSellOrder(@RequestParam String symbol, @RequestBody ApiKey apiKeyDto) {
		var clientOrderId = OrderIdUtil.generateOrderId();
		var orderRequest = BobooOrderRequestDto.builder()
			.symbol(symbol)
			.quantity(BigDecimal.valueOf(0.1))
			.side(OrderSide.SELL)
			.type(OrderType.LIMIT)
			.timeInForce(TimeInForce.GTC)
			.price(BigDecimal.valueOf(13.65))
			.newClientOrderId(clientOrderId)
			.build();
		return getBobooOrderResponseDto(symbol, apiKeyDto, clientOrderId, orderRequest);
	}

	private Mono<BobooOrderResponseDto> getBobooOrderResponseDto(@RequestParam String symbol,
		@RequestBody ApiKey apiKeyDto, String clientOrderId, BobooOrderRequestDto orderRequest) {
		return bobooExchangeService.requestOrder(orderRequest, apiKeyDto)
			.flatMap(bobooOrderResponseDto -> bobooExchangeService.getOpenOrders(symbol, apiKeyDto)
				.collectList()
				.zipWith(Mono.just(bobooOrderResponseDto))
			)
			.map(pair -> {
				List<BobooOpenOrdersDto> openOrders = pair.getT1();
				BobooOrderResponseDto orderResponse = pair.getT2();

				openOrders.stream()
					.filter(openOrder -> openOrder.getId().equals(clientOrderId))
					.findFirst()
					.ifPresent(bobooOpenOrdersDto -> log.info(
						"[BuyOrder] order has been made with symbol: {}, volume: {}, price: {}, side: {}, id: {}",
						orderRequest.getSymbol(), orderRequest.getQuantity(), orderRequest.getPrice(), orderRequest.getSide(),
						orderRequest.getNewClientOrderId()
					));
				return orderResponse;
			})
			.flatMap(orderResponse -> bobooExchangeService.cancelOrder(BobooCancelRequestDto.builder()
				.orderId(orderResponse.getOrderId())
				.build(), apiKeyDto)
				.map(cancelResponse -> orderResponse)
			)
			.doOnError(throwable -> log.error("[BuyOrder] Order failed with response of: {}",
				((WebClientResponseException) throwable).getResponseBodyAsString(), throwable));
	}

}
