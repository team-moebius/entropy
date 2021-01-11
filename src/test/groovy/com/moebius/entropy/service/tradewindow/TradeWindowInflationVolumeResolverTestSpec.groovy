package com.moebius.entropy.service.tradewindow

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.inflate.InflationConfig
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.order.OrderPosition
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.util.EntropyRandomUtils
import com.moebius.entropy.repository.InflationConfigRepository
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.tuple.Pair
import spock.lang.Shared
import spock.lang.Specification

import java.math.RoundingMode

class TradeWindowInflationVolumeResolverTestSpec extends Specification {
    def inflationConfigRepository = Mock(InflationConfigRepository)
    def entropyRandomUtils = Mock(EntropyRandomUtils)
    def sut = new TradeWindowInflationVolumeResolver(inflationConfigRepository, entropyRandomUtils)
    @Shared
    def symbol = "GTAX"
    @Shared
    def exchange = Exchange.BOBOO
    def market = new Market(exchange, symbol, TradeCurrency.USDT)

    def "Should get Random volume from configuration"() {
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

    def "Should get divided volume from request"() {
        given:
        def market = Stub(Market)
        def orderRange = Pair.of(1, 5)
        1 * inflationConfigRepository.getConfigFor(_ as Market) >> Stub(InflationConfig) {
            getAskMaxVolume() >> 1000
            getAskMinVolume() >> 10
            getBidMaxVolume() >> 1000
            getBidMinVolume() >> 10
        }
        1 * entropyRandomUtils.getRandomInteger(1, 5) >> 3
        1 * entropyRandomUtils.getRandomDecimal(10, 1000, 2) >> BigDecimal.valueOf(500)
        1 * entropyRandomUtils.getRandomDecimal(0.2f, _, 2) >> BigDecimal.valueOf(50)
        1 * entropyRandomUtils.getRandomDecimal(0.1f, _, 2) >> BigDecimal.valueOf(100)

        when:
        def result = sut.getDividedVolume(market, ORDER_POSITION, orderRange)

        then:
        CollectionUtils.isNotEmpty(result)
        result.stream().allMatch(dividedVolume -> dividedVolume >= 50 && dividedVolume <= 350)

        where:
        ORDER_POSITION << [OrderPosition.ASK, OrderPosition.BID]
    }
}
