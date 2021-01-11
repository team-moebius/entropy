package com.moebius.entropy.controller;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.OrderSide;
import com.moebius.entropy.domain.order.OrderType;
import com.moebius.entropy.domain.order.TimeInForce;
import com.moebius.entropy.dto.exchange.order.ApiKeyDto;
import com.moebius.entropy.dto.exchange.order.boboo.*;
import com.moebius.entropy.dto.order.DividedDummyOrderDto;
import com.moebius.entropy.service.exchange.boboo.BobooExchangeService;
import com.moebius.entropy.service.order.boboo.BobooDividedDummyOrderService;
import com.moebius.entropy.util.OrderIdUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerResponse;
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

	@PostMapping("/divided-dummy-order")
	public Mono<ServerResponse> testDividedDummyOrder(@RequestBody DividedDummyOrderDto dividedDummyOrderDto) {
		return bobooDividedDummyOrderService.executeDividedDummyOrders(dividedDummyOrderDto);
	}

	@DeleteMapping("/divided-dummy-order")
	public Mono<ServerResponse> testCancelDividedDummyOrder(@RequestParam String disposableId, @RequestBody Market market) {
		return bobooDividedDummyOrderService.cancelDividedDummyOrders(market, disposableId);
	}
	@PostMapping("/cancel-test-order")
	public Flux<BobooCancelResponse> cancelAllOpenOrders(@RequestParam String symbol, @RequestBody ApiKeyDto apiKeyDto){
		return bobooExchangeService.getOpenOrders(symbol, apiKeyDto)
				.map(bobooOpenOrdersDto -> BobooCancelRequest.builder()
							.orderId(bobooOpenOrdersDto.getInternalId())
							.build()
				)
				.flatMap(cancelRequest -> bobooExchangeService.cancelOrder(cancelRequest, apiKeyDto))
				.doOnError(throwable -> log.error("[BuyOrder] Order cancellation failed with response of: {}", ((WebClientResponseException.BadRequest)throwable).getResponseBodyAsString(), throwable))
				.doOnEach(signal -> Optional.ofNullable(signal.get())
						.ifPresent(response -> log.info(
								"[OrderTest] Cancel order with id :{}", response.getOrderId()
						)
				));
	}

	@PostMapping("/test-buy-order")
	public Mono<BobooOrderResponseDto> testBuyOrder(@RequestParam String symbol, @RequestBody ApiKeyDto apiKeyDto){
		var clientOrderId = OrderIdUtil.generateOrderId();
		var orderRequest = BobooOrderRequestDto.builder()
				.symbol(symbol)
				.quantity(BigDecimal.valueOf(0.1))
				.side(OrderSide.BUY)
				.type(OrderType.LIMIT)
				.timeInForce(TimeInForce.GTC)
				.price(BigDecimal.valueOf(0.01))
				.newClientOrderId(clientOrderId)
				.build();
		return getBobooOrderResponseDto(symbol, apiKeyDto, clientOrderId, orderRequest);
	}


	@PostMapping("/test-sell-order")
	public Mono<BobooOrderResponseDto> testSellOrder(@RequestParam String symbol, @RequestBody ApiKeyDto apiKeyDto){
		var clientOrderId = OrderIdUtil.generateOrderId();
		var orderRequest = BobooOrderRequestDto.builder()
				.symbol(symbol)
				.quantity(BigDecimal.valueOf(0.1))
				.side(OrderSide.SELL)
				.type(OrderType.LIMIT)
				.timeInForce(TimeInForce.GTC)
				.price(BigDecimal.valueOf(99999.99))
				.newClientOrderId(clientOrderId)
				.build();
		return getBobooOrderResponseDto(symbol, apiKeyDto, clientOrderId, orderRequest);
	}

	private Mono<BobooOrderResponseDto> getBobooOrderResponseDto(@RequestParam String symbol,
		@RequestBody ApiKeyDto apiKeyDto, String clientOrderId, BobooOrderRequestDto orderRequest) {
		return bobooExchangeService.requestOrder(orderRequest, apiKeyDto)
				.flatMap(bobooOrderResponseDto -> bobooExchangeService.getOpenOrders(symbol, apiKeyDto)
						.collectList()
						.zipWith(Mono.just(bobooOrderResponseDto))
				)
				.map(pair->{
					List<BobooOpenOrdersDto> openOrders = pair.getT1();
					BobooOrderResponseDto orderResponse = pair.getT2();

					openOrders.stream()
							.filter(openOrder -> openOrder.getId().equals(clientOrderId))
							.findFirst()
							.ifPresent(bobooOpenOrdersDto -> log.info(
									"[BuyOrder] order has been made with symbol: {}, volume: {}, price: {}, side: {}, id: {}",
									orderRequest.getSymbol(), orderRequest.getQuantity(), orderRequest.getPrice(), orderRequest.getSide(), orderRequest.getNewClientOrderId()
							));
					return orderResponse;
				})
				.flatMap(orderResponse -> bobooExchangeService.cancelOrder(BobooCancelRequest.builder()
						.orderId(orderResponse.getOrderId())
						.build(), apiKeyDto)
						.map(cancelResponse->orderResponse)
				)
				.doOnError(throwable -> log.error("[BuyOrder] Order failed with response of: {}", ((WebClientResponseException)throwable).getResponseBodyAsString(), throwable));
	}

}
