package com.moebius.entropy.service.order

import com.moebius.entropy.assembler.OrderBobooExchangeAssembler
import com.moebius.entropy.domain.*
import com.moebius.entropy.dto.exchange.order.boboo.BobooCancelRequest
import com.moebius.entropy.dto.exchange.order.boboo.BobooCancelResponse
import com.moebius.entropy.dto.exchange.order.boboo.BobooOrderRequestDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooOrderResponseDto
import com.moebius.entropy.service.exchange.BobooService
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@SuppressWarnings(['GroovyAssignabilityCheck', 'GroovyAccessibility'])
class OrderServiceTestSpec extends Specification {
    def mockExchangeService = Mock(BobooService)
    def mockAssembler = Mock(OrderBobooExchangeAssembler)
    def accessKey = "some_test_api_key"
    def secretKey = "some_test_secret_key"

    @Subject
    OrderService sut = new BobooOrderService(mockExchangeService, mockAssembler, accessKey, secretKey)

    @Shared
    def symbol = "GTAX"
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

        StepVerifier.create(sut.fetchAllOrdersFor(market))
                .assertNext({ assertOrderWithRequest(it, orderRequest) })
                .verifyComplete()

        StepVerifier.create(sut.fetchAutomaticOrdersFor(market))
                .assertNext({ assertOrderWithRequest(it, orderRequest) })
                .verifyComplete()

        StepVerifier.create(sut.fetchManualOrdersFor(market))
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

        StepVerifier.create(sut.fetchAllOrdersFor(market))
                .assertNext({ assertOrderWithRequest(it, orderRequest) })
                .verifyComplete()

        StepVerifier.create(sut.fetchManualOrdersFor(market))
                .assertNext({ assertOrderWithRequest(it, orderRequest) })
                .verifyComplete()

        StepVerifier.create(sut.fetchAutomaticOrdersFor(market))
                .verifyComplete()

        where:
        orderPosition << [OrderPosition.ASK, OrderPosition.BID]
    }

    def "Cancel order traced by OrderService"() {
        given:
        def orderTrackedByService = new Order(orderId, market, orderPosition, price, volume)
        addOrderList([orderTrackedByService])
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

    def "Cancel order not traced by OrderService"() {
        def orderShouldBeCancelled = new Order(orderId, market, orderPosition, price, volume)

        0 * mockExchangeService.cancelOrder(_, _)
        0 * mockAssembler.convertToCancelRequest(orderShouldBeCancelled)

        expect:
        StepVerifier.create(sut.cancelOrder(orderShouldBeCancelled))
                .verifyComplete()

        where:
        orderPosition << [OrderPosition.ASK, OrderPosition.BID]
    }

    @Unroll
    def "Update orders from exchange with condition of #comment"() {
        given:
        addOrderList(ordersTrackedByService)
        setAutomaticOrders(automaticOrderIds)

        expect:
        StepVerifier.create(sut.updateOrders(ordersReceivedFromExchange))
                .thenConsumeWhile({ true })
                .verifyComplete()

        StepVerifier.create(sut.fetchAllOrdersFor(market))
            .recordWith({[]})
            .thenConsumeWhile({true})
            .consumeRecordedWith({
                assert it.size() == ordersReceivedFromExchange.size()
                return it.stream().allMatch({updatedOrder ->
                    def orderFromExchange = ordersReceivedFromExchange.find {order-> updatedOrder.orderId == order.orderId}
                    assert orderFromExchange.price == updatedOrder.price
                    assert orderFromExchange.volume == updatedOrder.volume
                    true
                })
            })
            .verifyComplete()

        StepVerifier.create(sut.fetchAutomaticOrdersFor(market))
                .recordWith({[]})
                .thenConsumeWhile({true})
                .consumeRecordedWith({
                    assert it.stream().allMatch({order->
                        automaticOrderIdsAfterUpdate.contains(order.orderId)
                    })
                })
                .verifyComplete()

        where:
        ordersTrackedByService << [
                [createOrderWith("1", OrderPosition.ASK, 11.11, 111.111),
                 createOrderWith("2", OrderPosition.BID, 22.22, 222.222),
                 createOrderWith("3", OrderPosition.ASK, 33.33, 333.333),
                 createOrderWith("4", OrderPosition.BID, 44.44, 444.444),
                 createOrderWith("5", OrderPosition.ASK, 55.55, 555.555),
                ],
                [createOrderWith("1", OrderPosition.ASK, 11.11, 111.111),
                 createOrderWith("2", OrderPosition.BID, 22.22, 222.222),
                ],
                [createOrderWith("1", OrderPosition.ASK, 11.11, 111.111),
                 createOrderWith("2", OrderPosition.BID, 22.22, 222.222),
                 createOrderWith("3", OrderPosition.ASK, 33.33, 333.333),
                 createOrderWith("4", OrderPosition.BID, 44.44, 444.444),
                 createOrderWith("5", OrderPosition.ASK, 55.55, 555.555),
                ],
                [],
                []
        ]
        automaticOrderIds << [
                ["1", "2", "3", "4", "5"],
                ["1",],
                ["1", "4",],
                [],
                [],
        ]
        ordersReceivedFromExchange << [
                [createOrderWith("1", OrderPosition.ASK, 11.11, 111.111),
                 createOrderWith("2", OrderPosition.BID, 22.22, 222.222),
                 createOrderWith("3", OrderPosition.ASK, 33.33, 333.333),
                 createOrderWith("4", OrderPosition.BID, 44.44, 444.444),
                 createOrderWith("5", OrderPosition.ASK, 55.55, 555.555),
                ],
                [createOrderWith("1", OrderPosition.ASK, 11.11, 111.111),
                 createOrderWith("2", OrderPosition.BID, 22.22, 222.222),
                 createOrderWith("3", OrderPosition.ASK, 33.33, 333.333),
                 createOrderWith("4", OrderPosition.BID, 44.44, 444.444),
                 createOrderWith("5", OrderPosition.ASK, 55.55, 555.555),
                ],
                [createOrderWith("1", OrderPosition.ASK, 11.11, 111.111),
                 createOrderWith("5", OrderPosition.ASK, 55.55, 666.666),
                ],
                [],
                [createOrderWith("1", OrderPosition.ASK, 11.11, 111.111),
                 createOrderWith("2", OrderPosition.BID, 22.22, 222.222),
                 createOrderWith("3", OrderPosition.ASK, 33.33, 333.333),
                 createOrderWith("4", OrderPosition.BID, 44.44, 444.444),
                 createOrderWith("5", OrderPosition.ASK, 55.55, 555.555),
                ],
        ]
        automaticOrderIdsAfterUpdate << [
                new HashSet<>(["1", "2", "3", "4", "5"]),
                new HashSet<>(["1",]),
                new HashSet<>(["1",]),
                Collections.<String>emptySet(),
                Collections.<String>emptySet(),
        ]
        comment << [
                "Nothing is changed",
                "Someone made order directly from exchange service",
                "Some of orders are cancelled manually or transaction completed or partial completed",
                "There's no order on both sides",
                "Get orders from exchange when no orders are tracked"
        ]

    }

    boolean assertOrderWithRequest(Order order, OrderRequest orderRequest) {
        assert order.orderId != null
        assert order.market == orderRequest.market
        assert order.orderPosition == orderRequest.orderPosition
        assert order.price == price
        assert order.volume == volume
        return true
    }

    def addOrderList(List<Order> orders) {
        sut.orderListForSymbol.clear()
        sut.orderListForSymbol.computeIfAbsent(symbol, { [] })
            .addAll(orders)
    }

    Order createOrderWith(String orderId, OrderPosition orderPosition, float price, float volume) {
        return new Order(orderId, market, orderPosition, BigDecimal.valueOf(price), BigDecimal.valueOf(volume))
    }

    def setAutomaticOrders(List<String> orderIds) {
        sut.automaticOrderIds.clear()
        sut.automaticOrderIds.addAll(orderIds)
    }
}
