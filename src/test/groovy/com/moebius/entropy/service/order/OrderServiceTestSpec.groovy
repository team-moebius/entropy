package com.moebius.entropy.service.order

import com.moebius.entropy.assembler.BobooOrderExchangeAssembler
import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.order.Order
import com.moebius.entropy.domain.order.OrderPosition
import com.moebius.entropy.domain.order.OrderRequest
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.dto.exchange.order.boboo.BobooCancelRequest
import com.moebius.entropy.dto.exchange.order.boboo.BobooCancelResponse
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

    @Subject
    OrderService sut = new BobooOrderService(mockExchangeService, mockAssembler, disposableOrderRepository, accessKey, secretKey)

    @Shared
    def symbol = "GTAXUSDT"
    @Shared
    def market = new Market(Exchange.BOBOO, symbol, TradeCurrency.USDT)
    def price = BigDecimal.valueOf(11.11)
    def volume = BigDecimal.valueOf(123.123)
    @Shared
    def orderId = "12345"

    def orderResponse = Mock(BobooOrderResponseDto)

    def "After create automatic order then get automatic order list"() {
        given:
        def orderRequest = new OrderRequest(market, orderPosition, price, volume)
        1 * mockExchangeService.requestOrder(_, {
            it.accessKey==accessKey && it.secretKey == secretKey
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
            it.accessKey==accessKey && it.secretKey == secretKey
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
            it.accessKey==accessKey && it.secretKey == secretKey
        }) >> Mono.just(Mock(BobooCancelResponse))
        1 * mockAssembler.convertToCancelRequest(orderShouldBeCancelled) >> Mock(BobooCancelRequest)

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
