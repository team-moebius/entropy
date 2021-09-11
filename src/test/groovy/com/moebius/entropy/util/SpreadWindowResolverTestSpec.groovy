package com.moebius.entropy.util

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.order.OrderPosition
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.domain.trade.TradePrice
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.math.RoundingMode
import java.util.function.BinaryOperator

class SpreadWindowResolverTestSpec extends Specification {
    @Shared
    def marketPrice = new BigDecimal("11.35")
    @Shared
    def symbol = "GTAX2USDT"
    @Shared
    def exchange = Exchange.BOBOO
    def tradeCurrency = TradeCurrency.USDT

    def market = new Market(exchange, symbol, tradeCurrency, 2, 2)
    def spreadWindow = 5

    def randomUtil = Mock(EntropyRandomUtils)
    @Subject
    def sut = new SpreadWindowResolver(randomUtil)

    @Unroll
    def "Test merge prices into spread window when #comment"() {
        given:
        BinaryOperator<BigDecimal> operatorOnPrice = BigDecimal.&add
        def prices = (params['previousWindowPair'] as List<List>)
                .collect {
                    String priceStr = it.get(0)
                    int volume = it.get(1) as Integer
                    new TradePrice(OrderPosition.ASK, new BigDecimal(priceStr), new BigDecimal(volume))
                }

        Map<String, BigDecimal> desiredResult = (params['desiredPair'] as List<List>)
                .collectEntries {
                    [it.get(0), BigDecimal.valueOf(it.get(1) as Integer)]
                }

        when:
        def result = sut.mergeIntoTradeWindow(
                market, marketPrice.add(tradeCurrency.getPriceUnit()), spreadWindow, operatorOnPrice, prices
        )
        then:
        result == desiredResult

        where:
        params << [
                [
                        "previousWindowPair": [],
                        "desiredPair"       : [],
                        "comment"           : "prices are empty"
                ],
                [
                        //11.36, 11.41, 11.46, 11.51, 11.56, 11.61, 11.66, 11.71, 11.76, 11.81, 11.86, 11.91, 11.96, 12.01,
                        "previousWindowPair": [["11.36", 123], ["11.39", 123], ["11.43", 124], ["11.47", 125], ["11.55", 126], ["11.56", 127], ["11.61", 128]],
                        "desiredPair"       : [["11.36", 246], ["11.41", 124], ["11.46", 125], ["11.51", 126], ["11.56", 127], ["11.61", 128]],
                        "comment"           : "prices are less than desired size"
                ],
                [
                        "previousWindowPair": [["11.36", 123], ["11.39", 123], ["11.43", 124], ["11.47", 125], ["11.55", 126], ["11.56", 127], ["11.61", 128], ["11.70", 128], ["11.72", 129], ["11.78", 130], ["11.85", 131]],
                        "desiredPair"       : [["11.36", 246], ["11.41", 124], ["11.46", 125], ["11.51", 126], ["11.56", 127], ["11.61", 128], ["11.66", 128], ["11.71", 129], ["11.76", 130], ["11.81", 131]],
                        "comment"           : "prices are greater than desired size"
                ],
        ]
        comment << [
                "prices are empty",
                "prices are less than desired size",
                "prices are greater than desired size",
        ]
    }


    @Unroll
    def "Test resolve prices for spreadWindow when #comment"() {
        given:
        Map<String, BigDecimal> previousWindow = (params.get("previousWindowPair") as List<List>)
                .collectEntries() {
                    [it.get(0), new BigDecimal(it.get(1) as Integer)]
                }
        def expectedPrices = (params.get("madeAskPrices") as List<String>)
                .collect { new BigDecimal(it) }

        BinaryOperator<BigDecimal> operatorOnPrice = BigDecimal.&add
        def startPrice = marketPrice.add(tradeCurrency.getPriceUnit())

        def requestBuilder = SpreadWindowResolveRequest.builder()
        if (params.get("minVolumeStr") != null) {
            def minVolume = new BigDecimal(params.get("minVolumeStr") as String)
            requestBuilder.minimumVolume(minVolume)
        }
        def request = requestBuilder
                .count(8)
                .startPrice(startPrice).operationOnPrice(operatorOnPrice)
                .spreadWindow(spreadWindow).priceUnit(tradeCurrency.getPriceUnit())
                .previousWindow(previousWindow)
                .build()

        when:
        def resolvedPrices = sut.resolvePrices(request)

        then:
        randomUtil.getRandomDecimal(_ as BigDecimal, _ as BigDecimal, _ as Integer) >> { BigDecimal min, BigDecimal max, int decimalPlaces ->
            return max.add(min).divide(BigDecimal.valueOf(2L)).setScale(decimalPlaces, RoundingMode.HALF_UP)
        }
        resolvedPrices == expectedPrices

        where:
        params << [
                [
                        "minVolumeStr"      : null,
                        "previousWindowPair": [],
                        "madeAskPrices"     : ["11.39", "11.44", "11.49", "11.54", "11.59", "11.64", "11.69", "11.74"],
                        "comment"           : "previous window is empty and minVolume is not designated"
                ],
                [
                        "minVolumeStr"      : "99.9999",
                        "previousWindowPair": [],
                        "madeAskPrices"     : ["11.39", "11.44", "11.49", "11.54", "11.59", "11.64", "11.69", "11.74"],
                        "comment"           : "previous window is empty"
                ],
                [
                        "minVolumeStr"      : "99.9999",
                        "previousWindowPair": [["11.36", 246], ["11.41", 124], ["11.46", 125], ["11.51", 126], ["11.56", 127], ["11.61", 128]],
                        "madeAskPrices"     : ["11.69", "11.74"],
                        "comment"           : "previous window is less than desired size"
                ],
                [
                        "minVolumeStr"      : "99.9999",
                        "previousWindowPair": [["11.36", 246], ["11.41", 124], ["11.46", 125], ["11.51", 126], ["11.56", 127], ["11.61", 128], ["11.66", 128], ["11.71", 129], ["11.76", 130], ["11.81", 131]],
                        "madeAskPrices"     : [],
                        "comment"           : "previous window is greater than desired size"
                ]
        ]
        comment << [
                "previous window is empty and minVolume is not designated",
                "previous window is empty",
                "previous window is less than desired size",
                "previous window is greater than desired size"
        ]
    }
}
