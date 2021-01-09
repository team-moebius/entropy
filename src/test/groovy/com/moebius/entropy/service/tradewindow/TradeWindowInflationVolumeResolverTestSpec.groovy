package com.moebius.entropy.service.tradewindow

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.InflationConfig
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.OrderType
import com.moebius.entropy.domain.TradeCurrency
import com.moebius.entropy.service.order.OrderQuantityService
import com.moebius.entropy.service.tradewindow.repository.InflationConfigRepository
import spock.lang.Shared
import spock.lang.Specification

class TradeWindowInflationVolumeResolverTestSpec extends Specification {
    def inflationConfigRepository = Mock(InflationConfigRepository)
    def orderQuantityService = Mock(OrderQuantityService)
    def sut = new TradeWindowInflationVolumeResolver(inflationConfigRepository, orderQuantityService)
    @Shared
    def symbol = "GTAX"
    @Shared
    def exchange = Exchange.BOBOO
    def market = new Market(exchange, symbol, TradeCurrency.USDT)

    def "Get Random volume fron configuration"() {
        given:
        def desiredMaxVolume
        def desiredMinVolume

        if (orderType == OrderType.ASK){
            desiredMaxVolume = askMaxVolume
            desiredMinVolume = askMinVolume
        } else {
            desiredMaxVolume = bidMaxVolume
            desiredMinVolume = bidMinVolume
        }
        def randomlyPickedVolume = BigDecimal.valueOf(123.123)


        when:
        def resolvedVolume = sut.getInflationVolume(market, orderType)

        then:
        1 * inflationConfigRepository.getConfigFor(market) >> InflationConfig.builder()
                .bidMaxVolume(bidMaxVolume)
                .bidMinVolume(bidMinVolume)
                .askMaxVolume(askMaxVolume)
                .askMinVolume(askMinVolume)
                .build()
        1 * orderQuantityService.getRandomQuantity(desiredMinVolume.toFloat(), desiredMaxVolume.toFloat(), 2) >> randomlyPickedVolume
        resolvedVolume == randomlyPickedVolume

        where:
        bidMinVolume    |   bidMaxVolume        |   askMinVolume    |   askMaxVolume    |   orderType
        50.0            |   673482.231          |   100.0           |   1500.1          |   OrderType.ASK
        10.231          |   999556.1232         |   99.231          |   123556.1232     |   OrderType.ASK
        0.0             |   10.2312             |   0.0             |   0.0             |   OrderType.ASK
        12312.2         |   123123.231          |   -12312.2        |   -123123.231     |   OrderType.ASK
        100.0           |   1500.1              |   50.0            |   673482.231      |   OrderType.BID
        99.231          |   123556.1232         |   10.231          |   999556.1232     |   OrderType.BID
        0.0             |   0.0                 |   0.0             |   10.2312         |   OrderType.BID
        -12312.2        |   -123123.231         |   12312.2         |   123123.231      |   OrderType.BID

    }
}
