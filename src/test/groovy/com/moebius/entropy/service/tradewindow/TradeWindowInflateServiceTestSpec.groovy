package com.moebius.entropy.service.tradewindow


import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.inflate.InflateRequest
import com.moebius.entropy.domain.inflate.InflationConfig
import com.moebius.entropy.domain.order.Order
import com.moebius.entropy.domain.order.OrderPosition
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.domain.trade.TradePrice
import com.moebius.entropy.domain.trade.TradeWindow
import com.moebius.entropy.repository.InflationConfigRepository
import com.moebius.entropy.service.order.OrderService
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.util.stream.Collectors

@SuppressWarnings('GroovyAssignabilityCheck')
class TradeWindowInflateServiceTestSpec extends Specification {
    @Shared
    def marketPrice = new BigDecimal("11.35")
    @Shared
    def priceChangeUnit = TradeCurrency.USDT.getPriceUnit()
    @Shared
    def symbol = "GTAXUSDT"
    @Shared
    def exchange = Exchange.BOBOO
    def tradeWindowService = Mock(TradeWindowQueryService)
    def inflationConfigRepository = Mock(InflationConfigRepository)
    def orderService = Mock(OrderService)
    def inflationVolumeResolver = Mock(TradeWindowInflationVolumeResolver)
    def mockEventListener = Mock(BobooTradeWindowChangeEventListener)

    @Subject
    TradeWindowInflateService sut = new TradeWindowInflateService(
            tradeWindowService, inflationConfigRepository, orderService, inflationVolumeResolver, mockEventListener
    )

    def market = new Market(exchange, symbol, TradeCurrency.USDT)
    def inflateRequest = new InflateRequest(market)
    def inflationConfig = InflationConfig.builder()
            .askCount(8).bidCount(9).enable(true)
            .build()


//    1. Event를 Parameter로 받고(Event data에 Exchange와 Symbol 받음)
//    2. Trade Window 를 가져오고 여기서 개입이 필요한지 판단(매수 매도 각각 호가 수가 설정된 값 미만인지 판단)
//    3. 개입이 필요하다면 내 Order를 가져오고 거기에서 취소해야할 주문 선별
//    4. 3에서 선별된 주문을 취소
//    5. 설정된 호가 수를 채우기 위해 주문을 생성(여기서 부터는 ETP-4,5번 기능으로 delegation)
    @Unroll
    def "Test when #comment"() {
        given:
        def askInflationVolume = BigDecimal.valueOf(99.9999)
        def bidInflationVolume = BigDecimal.valueOf(111.1111)

        def askTradeWindow = tradeWindow(askVolumeForTradeWindow, OrderPosition.ASK)
        def bidTradeWindow = tradeWindow(bidVolumeForTradeWindow, OrderPosition.BID)
        def tradeWindow = new TradeWindow(askTradeWindow, bidTradeWindow)

        tradeWindowService.fetchTradeWindow(market) >> Mono.just(tradeWindow)
        tradeWindowService.getMarketPrice(market) >> marketPrice

        def askedOrders = orders(askedOrderVolumes, OrderPosition.ASK)
        def biddenOrders = orders(biddenOrderVolumes, OrderPosition.BID)
        orderService.fetchAutomaticOrdersFor(market) >> Flux.fromIterable(askedOrders + biddenOrders)

        inflationConfigRepository.getConfigFor(market) >> inflationConfig

        inflationVolumeResolver.getInflationVolume(market, OrderPosition.ASK) >> askInflationVolume
        inflationVolumeResolver.getInflationVolume(market, OrderPosition.BID) >> bidInflationVolume

        def madeAskedPrices = priceUnitMultipliersForAskOrdersShouldBeMade.stream().map({ multiplier ->
            def price = marketPrice + (priceChangeUnit * multiplier)
            orderService.requestOrder({
                it.market == market && it.orderPosition == OrderPosition.ASK \
                          && it.price == price && it.volume == askInflationVolume
            }) >> Mono.just(
                    new Order("${multiplier}", market, OrderPosition.ASK, price, askInflationVolume)
            )
            return price
        }).collect(Collectors.toList())

        def madeBidPrices = priceUnitMultipliersForBidOrdersShouldBeMade.stream().map({ multiplier ->
            def price = marketPrice - priceChangeUnit * multiplier
            orderService.requestOrder({
                it.market == market && it.orderPosition == OrderPosition.BID \
                          && it.price == price && it.volume == bidInflationVolume
            }) >> Mono.just(
                    new Order("${multiplier}", market, OrderPosition.BID, price, bidInflationVolume)
            )
            return price
        }).collect(Collectors.toList())

        def cancelledAskedPrices = priceUnitMultipliersForAskOrdersShouldBeCanceled.stream().map({ int multiplier ->
            def price = marketPrice + (priceChangeUnit * multiplier)
            1 * orderService.cancelOrder({
                it.market == market && it.orderPosition == OrderPosition.ASK  \
                         && it.price == price
            }) >> Mono.just(
                    new Order("${multiplier}", market, OrderPosition.ASK, price, 1)
            )
            return price
        }).collect(Collectors.toList())

        def cancelledBiddenPrices = priceUnitMultipliersForBidOrdersShouldBeCanceled.stream().map({ int multiplier ->
            def price = marketPrice - priceChangeUnit * multiplier
            1 * orderService.cancelOrder({
                it.market == market && it.orderPosition == OrderPosition.BID  \
                         && it.price == price
            }) >> Mono.just(
                    new Order("${multiplier}", market, OrderPosition.BID, price, 1)
            )
            return price
        }).collect(Collectors.toList())

        expect:
        StepVerifier.create(sut.inflateTrades(inflateRequest))
                .assertNext({
                    println("Case: " + comment)
                    println("Ask Orders should be made" + madeAskedPrices)
                    println("Ask Orders actually made: " + it.createdAskOrderPrices)

                    println("Bid Orders should be made" + madeBidPrices)
                    println("Bid Orders actually made" + it.createdBidOrderPrices)


                    println("Ask Orders should be cancelled" + cancelledAskedPrices)
                    println("Ask Orders actually cancelled: " + it.cancelledAskOrderPrices)

                    println("Bid Orders should be cancelled" + cancelledBiddenPrices)
                    println("Bid Orders actually cancelled" + it.cancelledBidOrderPrices)

                    it.getCreatedAskOrderPrices() == madeAskedPrices \
                        && it.getCreatedBidOrderPrices() == madeBidPrices \
                        && it.getCancelledAskOrderPrices() == cancelledAskedPrices \
                        && it.getCancelledBidOrderPrices() == cancelledBiddenPrices

                })
                .verifyComplete()


        where:
        askVolumeForTradeWindow << [
                [],
                [123, 124, 125, 126, 127, 128],
                [123, 124, 125, 126, 127, 128],
                [123, 124, 125, 126, 127, 128],
                [123, 124, 125, 126, 127, 128, 129, 130, 131, 132],
                [123, 124, 125, 126, 127, 128, 129, 130, 131, 132],
                [123, 124, 125, 126, 127, 128, 129, 130, 131, 132],
                [123, 124, 125, 126, 127, 128, 129, 130, 131, 132],
                [123, 124, 125, 126, 127, 128, 129, 130, 131, 132],
                [123, 124, 125, 126, 127, 128, 129, 130, 131, 132],
                [123, 124, 125, 126, 127, 128],
                [123, 124, 125, 126, 127, 128],
                [123, 124, 125, 126, 127, 128],
                [123, 0, 125, 126, 127, 128, 129, 130, 131, 132],

        ]
        bidVolumeForTradeWindow << [
                [],
                [201, 202, 203, 204, 205, 206],
                [201, 202, 203, 204, 205, 206],
                [201, 202, 203, 204, 205, 206],
                [201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211],
                [201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211],
                [201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211],
                [201, 202, 203, 204, 205, 206],
                [201, 202, 203, 204, 205, 206],
                [201, 202, 203, 204, 205, 206],
                [201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211],
                [201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211],
                [201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211],
                [201, 202, 0, 204, 205, 206, 207, 208, 209, 210, 211],
        ]

        askedOrderVolumes << [
                [],
                [0, 0, 0, 126, 127, 128],
                [],
                [123, 124, 125, 126, 127, 128],
                [0, 0, 0, 0, 0, 0, 0, 0, 131, 132],
                [123, 124, 125, 126, 127, 128, 129, 130, 131, 132],
                [],
                [0, 0, 0, 0, 0, 0, 0, 0, 131, 132],
                [123, 124, 125, 126, 127, 128, 129, 130, 131, 132],
                [],
                [0, 0, 0, 126, 127, 128],
                [123, 124, 125, 126, 127, 128],
                [],
                [123, 124, 125, 126, 127, 128, 129, 130, 131, 132],
        ]
        biddenOrderVolumes << [
                [],
                [0, 0, 0, 0, 205, 206],
                [],
                [201, 202, 203, 204, 205, 206],
                [0, 0, 0, 0, 0, 0, 0, 0, 0, 210, 211],
                [201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211],
                [],
                [0, 0, 0, 0, 205, 206],
                [],
                [201, 202, 203, 204, 205, 206],
                [0, 0, 0, 0, 0, 0, 0, 0, 0, 210, 211],
                [201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211],
                [],
                [201, 202, 0, 204, 205, 206, 207, 208, 209, 210, 211],
        ]
        priceUnitMultipliersForAskOrdersShouldBeMade << [
                [1, 2, 3, 4, 5, 6, 7, 8],
                [7, 8],
                [7, 8],
                [7, 8],
                [],
                [],
                [],
                [],
                [],
                [],
                [7, 8],
                [7, 8],
                [7, 8],
                [2],
        ]
        priceUnitMultipliersForBidOrdersShouldBeMade << [
                [0, 1, 2, 3, 4, 5, 6, 7, 8],
                [6, 7, 8],
                [6, 7, 8],
                [6, 7, 8],
                [],
                [],
                [],
                [6, 7, 8],
                [6, 7, 8],
                [6, 7, 8],
                [],
                [],
                [],
                [2],
        ]
        priceUnitMultipliersForAskOrdersShouldBeCanceled << [
                [],
                [],
                [],
                [],
                [10],
                [10],
                [],
                [10],
                [10],
                [],
                [],
                [],
                [],
                [10],
        ]
        priceUnitMultipliersForBidOrdersShouldBeCanceled << [
                [],
                [],
                [],
                [],
                [10],
                [10],
                [],
                [],
                [],
                [],
                [10],
                [10],
                [],
                [10],
        ]
        comment << [
                "trade window is empty on first start",
                "both trade window count less than objective count",
                "both trade window count less than objective count without automatic orders",
                "both trade window count less than objective count and all orders are automatic orders",
                "both trade window count larger than objective count",
                "both trade window count larger than objective count and all orders are automatic orders",
                "both trade window count larger than objective count without no automatic orders",
                "ask count over and bid count less than objective",
                "ask count over and bid count less than objective and all orders are automatic orders",
                "ask count over and bid count less than objective without no automatic orders",
                "ask count less and bid count over than objective",
                "ask count less and bid count over than objective and all orders are automatic orders",
                "ask count less and bid count over than objective without no automatic orders",
                "both trade window count larger than objective count but there's some missed price",
        ]

    }

    List<TradePrice> tradeWindow(List<Integer> volumes, OrderPosition orderPosition) {
        return (0..<volumes.size()).stream()
                .filter({ index ->
                    volumes[index] != 0
                })
                .map({ index ->
                    def priceMultiplier = orderPosition == OrderPosition.BID ? -index : index + 1
                    def volume = volumes[index]

                    BigDecimal price = marketPrice + priceChangeUnit * priceMultiplier
                    return new TradePrice(orderPosition, price, volume)
                })
                .collect(Collectors.toList())
    }

    List<Order> orders(List<Integer> volumes, OrderPosition orderPosition) {
        return (0..<volumes.size()).stream()
                .filter({ index ->
                    volumes[index] != 0
                })
                .map({ index ->
                    def priceMultiplier = orderPosition == OrderPosition.BID ? -index : index + 1
                    def volume = volumes[index]
                    BigDecimal price = marketPrice + priceChangeUnit * priceMultiplier
                    return new Order("${index}", market, orderPosition, price, volume)
                })
                .collect(Collectors.toList())
    }
}
