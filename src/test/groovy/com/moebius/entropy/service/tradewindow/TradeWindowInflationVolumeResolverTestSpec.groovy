package com.moebius.entropy.service.tradewindow

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.InflationConfig
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.OrderPosition
import com.moebius.entropy.domain.TradeCurrency
import com.moebius.entropy.util.EntropyRandomUtils
import com.moebius.entropy.service.tradewindow.repository.InflationConfigRepository
import spock.lang.Shared
import spock.lang.Specification

class TradeWindowInflationVolumeResolverTestSpec extends Specification {
    def inflationConfigRepository = Mock(InflationConfigRepository)
    def entropyRandomUtils = Mock(EntropyRandomUtils)
    def sut = new TradeWindowInflationVolumeResolver(inflationConfigRepository, entropyRandomUtils)
    @Shared
    def symbol = "GTAX"
    @Shared
    def exchange = Exchange.BOBOO
    def market = new Market(exchange, symbol, TradeCurrency.USDT)

    def "Get Random volume fron configuration"() {
        given:
        def desiredMaxVolume
        def desiredMinVolume

        if (orderPosition == OrderPosition.ASK){
            desiredMaxVolume = askMaxVolume
            desiredMinVolume = askMinVolume
        } else {
            desiredMaxVolume = bidMaxVolume
            desiredMinVolume = bidMinVolume
        }
        def randomlyPickedVolume = BigDecimal.valueOf(123.123)


        when:
        def resolvedVolume = sut.getInflationVolume(market, orderPosition)

        then:
        1 * inflationConfigRepository.getConfigFor(market) >> InflationConfig.builder()
                .bidMaxVolume(bidMaxVolume)
                .bidMinVolume(bidMinVolume)
                .askMaxVolume(askMaxVolume)
                .askMinVolume(askMinVolume)
                .build()
        1 * entropyRandomUtils.getRandomDecimal(desiredMinVolume.toFloat(), desiredMaxVolume.toFloat(), 2) >> randomlyPickedVolume
        resolvedVolume == randomlyPickedVolume

        where:
        bidMinVolume    |   bidMaxVolume        |   askMinVolume    |   askMaxVolume    |   orderPosition
        50.0            |   673482.231          |   100.0           |   1500.1          |   OrderPosition.ASK
        10.231          |   999556.1232         |   99.231          |   123556.1232     |   OrderPosition.ASK
        0.0             |   10.2312             |   0.0             |   0.0             |   OrderPosition.ASK
        12312.2         |   123123.231          |   -12312.2        |   -123123.231     |   OrderPosition.ASK
        100.0           |   1500.1              |   50.0            |   673482.231      |   OrderPosition.BID
        99.231          |   123556.1232         |   10.231          |   999556.1232     |   OrderPosition.BID
        0.0             |   0.0                 |   0.0             |   10.2312         |   OrderPosition.BID
        -12312.2        |   -123123.231         |   12312.2         |   123123.231      |   OrderPosition.BID

    }
}
