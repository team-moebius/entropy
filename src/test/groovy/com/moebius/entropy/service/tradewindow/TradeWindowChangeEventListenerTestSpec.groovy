package com.moebius.entropy.service.tradewindow

import com.moebius.entropy.assembler.TradeWindowAssembler
import com.moebius.entropy.assembler.TradeWindowAssemblerFactory
import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.inflate.InflationResult
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.domain.trade.TradeWindow
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto
import reactor.core.publisher.Flux
import spock.lang.Specification
import spock.lang.Subject

class TradeWindowChangeEventListenerTestSpec extends Specification {
	def assemblerFactory = Mock(TradeWindowAssemblerFactory)
	def commandService = Mock(TradeWindowCommandService)
	def inflateService = Mock(TradeWindowInflateService)

	@Subject
	def sut = new TradeWindowChangeEventListener(commandService, inflateService, assemblerFactory)

	def "On any changes on trade window"() {
		given:
		def orderBook = Stub(BobooOrderBookDto) {
			getSymbol() >> "GTAX2USDT"
		}
		def tradeWindow = new TradeWindow([], [])
		def market = new Market(Exchange.BOBOO, "GTAX2USDT", TradeCurrency.USDT, 2, 2)
		def marketPrice = BigDecimal.valueOf(123.123)

		when:
		sut.inflateOrdersOnTradeWindowChange(orderBook)

		then:
		1 * assemblerFactory.getTradeWindowAssembler(_ as Exchange) >> Stub(TradeWindowAssembler) {
			assembleTradeWindow(orderBook) >> tradeWindow
			extractMarket(orderBook) >> market
			extractMarketPrice(orderBook) >> marketPrice
		}
		1 * commandService.saveCurrentTradeWindow(market, marketPrice, tradeWindow)
		1 * inflateService.inflateOrders({ it.market == market }) >> Flux.just(Stub(InflationResult))
	}

	def "On failed to change on trade window"() {
		def orderBook = Stub(BobooOrderBookDto)
		def tradeWindow = null
		def market = new Market(Exchange.BOBOO, "GTAX2USDT", TradeCurrency.USDT, 2, 2)
		def marketPrice = BigDecimal.valueOf(123.123)

		when:
		sut.inflateOrdersOnTradeWindowChange(orderBook)

		then:
		0 * assemblerFactory.getTradeWindowAssembler(_ as Exchange) >> Stub(TradeWindowAssembler) {
			assembleTradeWindow(orderBook) >> tradeWindow
			extractMarket(orderBook) >> market
			extractMarketPrice(orderBook) >> marketPrice
		}
		0 * commandService.saveCurrentTradeWindow(_, _, _)
		0 * inflateService.inflateOrders(_)
	}
}
