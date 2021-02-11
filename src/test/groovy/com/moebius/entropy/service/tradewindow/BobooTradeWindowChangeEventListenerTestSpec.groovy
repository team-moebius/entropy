package com.moebius.entropy.service.tradewindow

import com.moebius.entropy.assembler.TradeWindowAssembler
import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.inflate.InflateRequest
import com.moebius.entropy.domain.inflate.InflationResult
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.domain.trade.TradeWindow
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto
import reactor.core.publisher.Mono
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Subject

class BobooTradeWindowChangeEventListenerTestSpec extends Specification {
    def commandService = Mock(TradeWindowCommandService)
    def assembler = Mock(TradeWindowAssembler)
    def inflateService = Mock(TradeWindowInflateService) {
        inflateTrades(_ as InflateRequest) >> Mono.just(Stub(InflationResult))
    }
    @Subject
    def sut = new BobooTradeWindowChangeEventListener(commandService, assembler,)

    def setup(){
        sut.setTradeWindowInflateService(inflateService)
    }

    @Ignore // FIXME : I will fix this soon.
    def "On any changes on trade window"() {
        given:
        def orderBook = Mock(BobooOrderBookDto)
        def tradeWindow = new TradeWindow([], [])
        def market = new Market(Exchange.BOBOO, "GTAXUSDT", TradeCurrency.USDT)
        def marketPrice = BigDecimal.valueOf(123.123)
        assembler.assembleTradeWindow(orderBook) >> tradeWindow
        assembler.extractMarket(orderBook) >> market
        assembler.extractMarketPrice(orderBook) >> marketPrice

        when:
        sut.inflateOnTradeWindowChange(orderBook)

        then:
        1 * commandService.saveCurrentTradeWindow(market, marketPrice, tradeWindow)
        1 * inflateService.inflateTrades({ it.market == market })
    }

    def "On failed to change on trade window"() {
        def orderBook = Mock(BobooOrderBookDto)
        def tradeWindow = null
        def market = new Market(Exchange.BOBOO, "GTAXUSDT", TradeCurrency.USDT)
        def marketPrice = BigDecimal.valueOf(123.123)
        assembler.assembleTradeWindow(orderBook) >> tradeWindow
        assembler.extractMarket(orderBook) >> market
        assembler.extractMarketPrice(orderBook) >> marketPrice

        when:
        sut.inflateOnTradeWindowChange(orderBook)

        then:
        0 * commandService.saveCurrentTradeWindow(_, _, _)
        0 * inflateService.inflateTrades(_)
    }
}
