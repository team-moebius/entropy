package com.moebius.entropy.service.tradewindow

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.TradeCurrency
import com.moebius.entropy.domain.TradeWindow
import com.moebius.entropy.service.tradewindow.repository.TradeWindowRepository
import spock.lang.Specification
import spock.lang.Subject

class TradeWindowCommandServiceTestSpec extends Specification {
    def mockRepository = Mock(TradeWindowRepository)
    @Subject
    def sut = new TradeWindowCommandService(mockRepository)

    def "Test save command"() {
        given:
        def market = new Market(exchange, symbol, currency)
        when:
        sut.saveCurrentTradeWindow(market, marketPrice, tradeWindow)

        then:
        1 * mockRepository.saveTradeWindowForSymbol(market, tradeWindow)
        1 * mockRepository.savePriceForSymbol(market, marketPrice)

        where:
        exchange       | symbol | currency           | marketPrice | tradeWindow
        Exchange.BOBOO | "GTAX" | TradeCurrency.USDT | 123.12      | new TradeWindow([], [])
        Exchange.BOBOO | "BTC"  | TradeCurrency.USDT | 1543.12     | new TradeWindow([], [])
        Exchange.BOBOO | "GTAX" | TradeCurrency.KRW  | 123.12      | new TradeWindow([], [])
        Exchange.BOBOO | "BTC"  | TradeCurrency.KRW  | 1543.12     | new TradeWindow([], [])
    }


}
