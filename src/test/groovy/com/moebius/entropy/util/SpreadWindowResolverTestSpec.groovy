package com.moebius.entropy.util

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.order.OrderPosition
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.domain.trade.TradePrice
import org.apache.commons.lang3.tuple.Pair
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

        Map<String, TradePrice> desiredResult = new HashMap<>((params['desiredPair'] as List<List>)
                .collectEntries {
                    def priceVolumePair = it.get(1) as List
                    def tradePrice = new TradePrice(
                            OrderPosition.ASK,
                            new BigDecimal(priceVolumePair.get(0) as String),
                            new BigDecimal(priceVolumePair.get(1) as String)
                    )
                    [it.get(0) as String, tradePrice]
                }
        )

        when:
        def result = sut.mergeIntoTradeWindow(
                tradeCurrency.getPriceUnit(), marketPrice.add(tradeCurrency.getPriceUnit()), spreadWindow, operatorOnPrice, prices
        )
        then:
        result.size() == desiredResult.size()
        result.entrySet().forEach {
            def key = it.getKey()
            def resultItem = it.value
            assert desiredResult.containsKey(key)
            def desiredResultItem = desiredResult.get(key)
            assert resultItem.getOrderPosition() == desiredResultItem.getOrderPosition()
            assert resultItem.getUnitPrice() == desiredResultItem.getUnitPrice()
            assert resultItem.getVolume() == desiredResultItem.getVolume()
        }

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
                        "desiredPair"       : [["11.36", ["11.36", 123]], ["11.41", ["11.43", 124]], ["11.46", ["11.47", 125]], ["11.51", ["11.55", 126]], ["11.56", ["11.56", 127]], ["11.61", ["11.61", 128]]],
                        "comment"           : "prices are less than desired size"
                ],
                [
                        "previousWindowPair": [["11.36", 123], ["11.39", 123], ["11.43", 124], ["11.47", 125], ["11.55", 126], ["11.56", 127], ["11.61", 128], ["11.70", 128], ["11.72", 129], ["11.78", 130], ["11.85", 131]],
                        "desiredPair"       : [["11.36", ["11.36", 123]], ["11.41", ["11.43", 124]], ["11.46", ["11.47", 125]], ["11.51", ["11.55", 126]], ["11.56", ["11.56", 127]], ["11.61", ["11.61", 128]], ["11.66", ["11.70", 128]], ["11.71", ["11.72", 129]], ["11.76", ["11.78", 130]], ["11.81", ["11.85", 131]]],
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
        List<TradePrice> previousWindow = (params.get("previousWindowPair") as List<List>)
                .collect {
                    new TradePrice(
                            OrderPosition.ASK,
                            new BigDecimal(it.get(0) as String),
                            new BigDecimal(it.get(1) as Integer)
                    )
                }

        def expectedPrices = (params.get("madeAskPrices") as List<List>)
                .collect {
                    def price = new BigDecimal(it.get(0) as String)
                    def startVolume = it.get(1) as BigDecimal
                    Pair.of(price, startVolume)
                }

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
        def resolvedPrices = sut.resolvePriceMinVolumePair(request)

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
                        "madeAskPrices"     : [["11.44", BigDecimal.ZERO], ["11.49", BigDecimal.ZERO], ["11.54", BigDecimal.ZERO], ["11.59", BigDecimal.ZERO], ["11.64", BigDecimal.ZERO], ["11.69", BigDecimal.ZERO], ["11.74", BigDecimal.ZERO], ["11.79", BigDecimal.ZERO]],
                        "comment"           : "previous window is empty and minVolume is not designated"
                ],
                [
                        "minVolumeStr"      : "99.9999",
                        "previousWindowPair": [],
                        "madeAskPrices"     : [["11.44", BigDecimal.ZERO], ["11.49", BigDecimal.ZERO], ["11.54", BigDecimal.ZERO], ["11.59", BigDecimal.ZERO], ["11.64", BigDecimal.ZERO], ["11.69", BigDecimal.ZERO], ["11.74", BigDecimal.ZERO], ["11.79", BigDecimal.ZERO]],
                        "comment"           : "previous window is empty"
                ],
                [
                        "minVolumeStr"      : "99.9999",
                        "previousWindowPair": [["11.36", 246], ["11.41", 124], ["11.46", 125], ["11.51", 126], ["11.56", 127], ["11.61", 128]],
                        "madeAskPrices"     : [["11.41", new BigDecimal("-2.40001E1")], ["11.46", new BigDecimal("-2.50001E1")], ["11.51", new BigDecimal("-2.60001E1")], ["11.56", new BigDecimal("-2.70001E1")], ["11.61", new BigDecimal("-2.80001E1")], ["11.69", BigDecimal.ZERO], ["11.74", BigDecimal.ZERO], ["11.79", BigDecimal.ZERO]],
                        "comment"           : "previous window is less than desired size"
                ],
                [
                        "minVolumeStr"      : "99.9999",
                        "previousWindowPair": [["11.36", 246], ["11.41", 124], ["11.46", 125], ["11.51", 126], ["11.56", 127], ["11.61", 128], ["11.66", 128], ["11.71", 129], ["11.76", 130], ["11.81", 131]],
                        "madeAskPrices"     : [["11.41", new BigDecimal("-2.40001E1")], ["11.46", new BigDecimal("-2.50001E1")], ["11.51", new BigDecimal("-2.60001E1")], ["11.56", new BigDecimal("-2.70001E1")], ["11.61", new BigDecimal("-2.80001E1")], ["11.66", new BigDecimal("-2.80001E1")], ["11.71", new BigDecimal("-2.90001E1")], ["11.76", new BigDecimal("-3.00001E1")]],
                        "comment"           : "previous window is greater than desired size"
                ],
                [
                        "minVolumeStr"      : "99.9999",
                        "previousWindowPair": [["11.36", 50], ["11.42", 50], ["11.45", 60], ["11.46", 124], ["11.51", 126], ["11.56", 127], ["11.61", 128], ["11.66", 128], ["11.71", 129], ["11.76", 130], ["11.81", 131]],
                        "madeAskPrices"     : [["11.45", new BigDecimal("39.9999")], ["11.46", new BigDecimal("-2.40001E1")], ["11.51", new BigDecimal("-2.60001E1")], ["11.56", new BigDecimal("-2.70001E1")], ["11.61", new BigDecimal("-2.80001E1")], ["11.66", new BigDecimal("-2.80001E1")], ["11.71", new BigDecimal("-2.90001E1")], ["11.76", new BigDecimal("-3.00001E1")]],
                        "comment"           : "previous window is greater than desired size but first 2 orders are requested less than min volume"
                ]
        ]
        comment << [
                "previous window is empty and minVolume is not designated",
                "previous window is empty",
                "previous window is less than desired size",
                "previous window is greater than desired size",
                "previous window is greater than desired size but first 2 orders are requested less than min volume"
        ]
    }
}
