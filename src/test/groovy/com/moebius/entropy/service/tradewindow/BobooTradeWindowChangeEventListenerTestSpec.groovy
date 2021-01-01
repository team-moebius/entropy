package com.moebius.entropy.service.tradewindow

import com.moebius.entropy.assembler.TradeWindowAssembler
import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.TradeCurrency
import com.moebius.entropy.domain.TradeWindow
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto
import spock.lang.Specification
import spock.lang.Subject

class BobooTradeWindowChangeEventListenerTestSpec extends Specification {
    def commandService = Mock(TradeWindowCommandService)
    def assembler = Mock(TradeWindowAssembler)
    def inflateService = Mock(TradeWindowInflateService)
    @Subject
    def sut = new BobooTradeWindowChangeEventListener(commandService, assembler,)

    def setup(){
        sut.setTradeWindowInflateService(inflateService)
    }

    def "On any changes on trade window"() {
        given:
        def orderBook = Mock(BobooOrderBookDto)
        def tradeWindow = new TradeWindow([], [])
        def market = new Market(Exchange.BOBOO, "GTAX", TradeCurrency.USDT)
        def marketPrice = BigDecimal.valueOf(123.123)
        assembler.assembleTradeWindow(orderBook) >> tradeWindow
        assembler.extractMarket(orderBook) >> market
        assembler.extractMarketPrice(orderBook) >> marketPrice

        when:
        sut.onTradeWindowChange(orderBook)

        then:
        1 * commandService.saveCurrentTradeWindow(market, marketPrice, tradeWindow)
        1 * inflateService.inflateTrades({ it.market == market })
    }

    def "On failed to change on trade window"() {
        def orderBook = Mock(BobooOrderBookDto)
        def tradeWindow = null
        def market = new Market(Exchange.BOBOO, "GTAX", TradeCurrency.USDT)
        def marketPrice = BigDecimal.valueOf(123.123)
        assembler.assembleTradeWindow(orderBook) >> tradeWindow
        assembler.extractMarket(orderBook) >> market
        assembler.extractMarketPrice(orderBook) >> marketPrice

        when:
        sut.onTradeWindowChange(orderBook)

        then:
        0 * commandService.saveCurrentTradeWindow(_, _, _)
        0 * inflateService.inflateTrades(_)
    }
}
