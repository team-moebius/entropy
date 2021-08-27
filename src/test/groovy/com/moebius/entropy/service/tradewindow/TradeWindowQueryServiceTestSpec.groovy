package com.moebius.entropy.service.tradewindow

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.domain.trade.TradeWindow
import com.moebius.entropy.repository.TradeDataRepository
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

class TradeWindowQueryServiceTestSpec extends Specification {
    def repository = Mock(TradeDataRepository)
    @Subject
    TradeWindowQueryService sut = new TradeWindowQueryService(repository)

    def "test fetching tradeWindow and marketPrice"() {
        given:
        def market = new Market(Exchange.BOBOO, "GTAX2USDT", TradeCurrency.USDT, 2, 2)
        repository.getMarketPriceForSymbol(market) >> marketPrice
        repository.getTradeWindowForSymbol(market) >> tradeWindow

        expect:
        sut.getMarketPrice(market) == marketPrice

        def stepVerifier = StepVerifier.create(sut.getTradeWindowMono(market))
        if (tradeWindow != null) {
            stepVerifier.assertNext({
                it == tradeWindow
            })
                    .verifyComplete()
        } else {
            stepVerifier.verifyComplete()
        }

        where:
        tradeWindow             | marketPrice
        new TradeWindow([], []) | BigDecimal.valueOf(123.232)
        null                    | null
    }
}
