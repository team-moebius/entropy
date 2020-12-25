package com.moebius.entropy.service.tradewindow

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.TradeWindow
import com.moebius.entropy.service.tradewindow.repository.TradeWindowRepository
import spock.lang.Specification
import spock.lang.Subject

class TradeWindowCommandServiceTestSpec extends Specification {
    def mockRepository = Mock(TradeWindowRepository)
    @Subject
    def sut = new TradeWindowCommandService(mockRepository)

    def "Test save command"() {

        when:
        sut.saveCurrentTradeWindow(exchange, symbol, marketPrice, tradeWindow)

        then:
        1 * mockRepository.saveTradeWindowForSymbol(exchange, symbol, tradeWindow)
        1 * mockRepository.savePriceForSymbol(exchange, symbol, marketPrice)

        where:
        exchange       | symbol     | marketPrice | tradeWindow
        Exchange.BOBOO | "GTAXUSDT" | 123.12      | new TradeWindow([], [])
        Exchange.BOBOO | "BTCUSDT"  | 1543.12     | new TradeWindow([], [])
        Exchange.BOBOO | "GTAXKRW"  | 123.12      | new TradeWindow([], [])
        Exchange.BOBOO | "BTCKRW"   | 1543.12     | new TradeWindow([], [])
    }


}
