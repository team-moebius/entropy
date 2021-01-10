package com.moebius.entropy.assembler

import com.moebius.entropy.domain.*
import com.moebius.entropy.domain.order.Order
import com.moebius.entropy.domain.order.OrderPosition
import com.moebius.entropy.domain.order.OrderRequest
import com.moebius.entropy.domain.order.OrderSide
import com.moebius.entropy.domain.order.OrderStatus
import com.moebius.entropy.domain.order.OrderType
import com.moebius.entropy.domain.order.TimeInForce
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.dto.exchange.order.boboo.BobooOpenOrdersDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooOrderResponseDto
import org.apache.commons.lang3.StringUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class BobooOrderExchangeAssemblerTestSpec extends Specification {
	@Subject
	def sut = new BobooOrderExchangeAssembler()

	@Shared
	def symbol = "GTAX"
	@Shared
	def market = new Market(Exchange.BOBOO, symbol, TradeCurrency.USDT)
	def price = BigDecimal.valueOf(11.11)
	def volume = BigDecimal.valueOf(123.123)

	def "Convert OrderRequest to BobooOrderRequest"() {
		given:
		def orderRequest = new OrderRequest(market, orderPosition, price, volume)
		when:
		def bobooOrderRequest = sut.convertToOrderRequest(orderRequest)

		then:
		bobooOrderRequest.symbol == "${symbol}${market.tradeCurrency.name()}"
		bobooOrderRequest.quantity == volume
		bobooOrderRequest.side == orderSide
		bobooOrderRequest.type == OrderType.LIMIT
		bobooOrderRequest.timeInForce == TimeInForce.GTC
		bobooOrderRequest.price == price
		StringUtils.isNotEmpty(bobooOrderRequest.newClientOrderId)

		where:
		orderPosition     | orderSide
        OrderPosition.ASK | OrderSide.BUY
		OrderPosition.BID | OrderSide.SELL
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
				.type(OrderType.LIMIT)
				.side(OrderSide.SELL)
				.build()
		when:
		def order = sut.convertToOrder(bobooOrderResponse)

		then:
		order.orderId == bobooOrderResponse.orderId
		order.market.symbol == symbol
		order.market.exchange == market.getExchange()
		order.orderPosition == OrderPosition.BID
		order.price == bobooOrderResponse.price
		order.volume == bobooOrderResponse.origQty
	}

	def "Convert OrderCancelRequest to BobooOrderCancelRequest"() {
		given:
		def orderId = "1234566"
		def order = new Order(orderId, market, orderPosition, price, volume)

		when:
		def bobooCancelRequest = sut.convertToCancelRequest(order)

		then:
		bobooCancelRequest.orderId == orderId

		where:
		orderPosition << [
				OrderPosition.BID,
				OrderPosition.ASK
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
				.orderType(OrderType.LIMIT)
				.orderSide(orderSide)
				.internalId("12983789f89h23213")
				.build()
		when:
		def order = sut.convertExchangeOrder(bobooOrderResponse)

		then:
		order.orderId == bobooOrderResponse.internalId
		order.market.symbol == symbol
		order.market.exchange == market.exchange
		order.orderPosition == orderPosition
		order.price == BigDecimal.valueOf(bobooOrderResponse.price)
		order.volume == BigDecimal.valueOf(bobooOrderResponse.originalQuantity).subtract(BigDecimal.valueOf(bobooOrderResponse.executedQuantity))

		where:
		orderSide      | orderPosition
		OrderSide.SELL | OrderPosition.BID
		OrderSide.BUY  | OrderPosition.ASK
	}


}
