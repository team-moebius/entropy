package com.moebius.entropy.assembler

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.Order
import com.moebius.entropy.domain.OrderRequest
import com.moebius.entropy.domain.OrderType
import com.moebius.entropy.domain.TradeCurrency
import com.moebius.entropy.domain.order.OrderPosition
import com.moebius.entropy.domain.order.OrderSide
import com.moebius.entropy.domain.order.OrderStatus
import com.moebius.entropy.domain.order.TimeInForce
import com.moebius.entropy.dto.exchange.order.boboo.BobooOpenOrdersDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooOrderResponseDto
import org.apache.commons.lang3.StringUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class OrderBobooExchangeAssemblerTestSpec extends Specification {
    @Subject
    def sut = new OrderBobooExchangeAssembler()

    @Shared
    def symbol = "GTAX"
    @Shared
    def market = new Market(Exchange.BOBOO, symbol, TradeCurrency.USDT)
    def price = BigDecimal.valueOf(11.11)
    def volume = BigDecimal.valueOf(123.123)

    def "Convert OrderRequest to BobooOrderRequest"() {
        given:
        def orderRequest = new OrderRequest(market, orderType, price, volume)
        when:
        def bobooOrderRequest = sut.convertToOrderRequest(orderRequest)

        then:
        bobooOrderRequest.symbol == "${symbol}${market.tradeCurrency.name()}"
        bobooOrderRequest.quantity == volume
        bobooOrderRequest.side == orderSide
        bobooOrderRequest.type == OrderPosition.LIMIT
        bobooOrderRequest.timeInForce == TimeInForce.GTC
        bobooOrderRequest.price == price
        StringUtils.isNotEmpty(bobooOrderRequest.newClientOrderId)

        where:
        orderType     | orderSide
        OrderType.ASK | OrderSide.BUY
        OrderType.BID | OrderSide.SELL
    }

    def "Convert BobooOrderResultResponse to Order entity"() {
        given:
        def bobooOrderResponse = BobooOrderResponseDto.builder()
                .symbol("GTAXUSDT")
                .orderId("494736827050147840")
                .clientOrderId("157371322565051")
                .transactTime(1573713225668)
                .price(BigDecimal.valueOf(0.005452))
                .origQty(BigDecimal.valueOf(110))
                .executedQty(BigDecimal.valueOf(0))
                .status(OrderStatus.NEW)
                .timeInForce(TimeInForce.GTC)
                .type(OrderPosition.LIMIT)
                .side(OrderSide.SELL)
                .build()
        when:
        def order = sut.convertToOrder(bobooOrderResponse)

        then:
        order.orderId == bobooOrderResponse.clientOrderId
        order.market.symbol == symbol
        order.market.exchange == market.getExchange()
        order.orderType == OrderType.BID
        order.price == bobooOrderResponse.price
        order.volume == bobooOrderResponse.origQty
    }

    def "Convert OrderCancelRequest to BobooOrderCancelRequest"() {
        given:
        def orderId = "1234566"
        def order = new Order(orderId, market, orderType, price, volume)

        when:
        def bobooCancelRequest = sut.convertToCancelRequest(order)

        then:
        bobooCancelRequest.orderId == orderId

        where:
        orderType << [
                OrderType.BID,
                OrderType.ASK
        ]
    }

    def "Convert OpenOrder from Boboo to Order"() {
        given:
        def bobooOrderResponse = BobooOpenOrdersDto.builder()
                .symbol("GTAXUSDT")
                .id("157371322565051")
                .price(0.005452)
                .originalQuantity(110.0)
                .executedQuantity(93.2)
                .orderStatus(OrderStatus.NEW)
                .orderPosition(OrderPosition.LIMIT)
                .orderSide(orderSide)
                .build()
        when:
        def order = sut.convertExchangeOrder(bobooOrderResponse)

        then:
        order.orderId == bobooOrderResponse.id
        order.market.symbol == symbol
        order.market.exchange == market.exchange
        order.orderType == orderType
        order.price == BigDecimal.valueOf(bobooOrderResponse.price)
        order.volume == BigDecimal.valueOf(bobooOrderResponse.originalQuantity).subtract(BigDecimal.valueOf(bobooOrderResponse.executedQuantity))

        where:
        orderSide       | orderType
        OrderSide.SELL  | OrderType.BID
        OrderSide.BUY   | OrderType.ASK
    }


}
