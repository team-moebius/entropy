package com.moebius.entropy.service.order

import com.moebius.entropy.assembler.boboo.BobooOrderExchangeAssembler
import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.Symbol
import com.moebius.entropy.domain.order.ApiKey
import com.moebius.entropy.domain.order.Order
import com.moebius.entropy.domain.order.OrderPosition
import com.moebius.entropy.domain.order.OrderRequest
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.dto.exchange.order.boboo.BobooCancelRequestDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooCancelResponseDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooOrderRequestDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooOrderResponseDto
import com.moebius.entropy.repository.DisposableOrderRepository
import com.moebius.entropy.service.exchange.boboo.BobooExchangeService
import com.moebius.entropy.service.order.boboo.BobooOrderService
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

@SuppressWarnings(['GroovyAssignabilityCheck', 'GroovyAccessibility'])
class OrderServiceTestSpec extends Specification {
	def mockExchangeService = Mock(BobooExchangeService)
	def mockAssembler = Mock(BobooOrderExchangeAssembler)
	def disposableOrderRepository = Mock(DisposableOrderRepository)
	def accessKey = "some_test_api_key"
	def secretKey = "some_test_secret_key"
	def apiKey = Stub(ApiKey) {
		getAccessKey() >> accessKey
		getSecretKey() >> secretKey
	}
	def apiKeys = [(Exchange.BOBOO): [(Symbol.GTAX2USDT): apiKey, (Symbol.MOIUSDT): apiKey],
				   (Exchange.BIGONE): [(Symbol.OAUSDT): apiKey]]

	@Subject
	OrderService sut = new BobooOrderService(mockExchangeService, mockAssembler, apiKeys, disposableOrderRepository)

	@Shared
	def symbol = "GTAX2USDT"
	@Shared
	def market = new Market(Exchange.BOBOO, symbol, TradeCurrency.USDT, 2, 2)
	def price = BigDecimal.valueOf(11.11)
	def volume = BigDecimal.valueOf(123.123)
	@Shared
	def orderId = "12345"

	def orderResponse = Mock(BobooOrderResponseDto)

	def "After create automatic order then get automatic order list"() {
		given:
		def orderRequest = new OrderRequest(market, orderPosition, price, volume)
		1 * mockExchangeService.requestOrder(_, {
			it.accessKey == accessKey && it.secretKey == secretKey
		}) >> Mono.just(orderResponse)
		1 * mockAssembler.convertToOrderRequest(orderRequest) >> Mock(BobooOrderRequestDto)
		1 * mockAssembler.convertToOrder(orderResponse) >> new Order(
				orderId, market, orderPosition, price, volume
		)

		expect:
		StepVerifier.create(sut.requestOrder(orderRequest))
				.assertNext({ assertOrderWithRequest(it, orderRequest) })
				.verifyComplete()

		where:
		orderPosition << [OrderPosition.ASK, OrderPosition.BID]
	}

	def "After create manual order then get automatic order list"() {
		given:
		def orderRequest = new OrderRequest(market, orderPosition, price, volume)
		1 * mockExchangeService.requestOrder(_, {
			it.accessKey == accessKey && it.secretKey == secretKey
		}) >> Mono.just(orderResponse)
		1 * mockAssembler.convertToOrderRequest(orderRequest) >> Mock(BobooOrderRequestDto)
		1 * mockAssembler.convertToOrder(orderResponse) >> new Order(
				orderId, market, orderPosition, price, volume
		)

		expect:
		StepVerifier.create(sut.requestManualOrder(orderRequest))
				.assertNext({ assertOrderWithRequest(it, orderRequest) })
				.verifyComplete()


		where:
		orderPosition << [OrderPosition.ASK, OrderPosition.BID]
	}

	def "Cancel order traced by OrderService"() {
		given:
		def orderShouldBeCancelled = new Order(orderId, market, orderPosition, price, volume)

		1 * mockExchangeService.cancelOrder(_, {
			it.accessKey == accessKey && it.secretKey == secretKey
		}) >> Mono.just(Mock(BobooCancelResponseDto))
		1 * mockAssembler.convertToCancelRequest(orderShouldBeCancelled) >> Mock(BobooCancelRequestDto)

		expect:
		StepVerifier.create(sut.cancelOrder(orderShouldBeCancelled))
				.assertNext({
					it == orderShouldBeCancelled
				})
				.verifyComplete()

		where:
		orderPosition << [OrderPosition.ASK, OrderPosition.BID]
	}

	boolean assertOrderWithRequest(Order order, OrderRequest orderRequest) {
		assert order.orderId != null
		assert order.market == orderRequest.market
		assert order.orderPosition == orderRequest.orderPosition
		assert order.price == price
		assert order.volume == volume
		return true
	}

}
