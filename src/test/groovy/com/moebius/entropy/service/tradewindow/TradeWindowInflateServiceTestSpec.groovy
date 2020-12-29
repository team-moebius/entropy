package com.moebius.entropy.service.tradewindow

import com.moebius.entropy.domain.*
import com.moebius.entropy.service.order.OrderService
import com.moebius.entropy.service.tradewindow.repository.InflationConfigRepository
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
    def symbol = "GTAX"
    @Shared
    def exchange = Exchange.BOBOO
    def tradeWindowService = Mock(TradeWindowQueryService)
    def inflationConfigRepository = Mock(InflationConfigRepository)
    def orderService = Mock(OrderService)
    def inflationVolumeResolver = Mock(TradeWindowInflationVolumeResolver)

    @Subject
    TradeWindowInflateService sut = new TradeWindowInflateService(
            tradeWindowService, inflationConfigRepository, orderService, inflationVolumeResolver
    )

    def market = new Market(Exchange.BOBOO, symbol, TradeCurrency.USDT)
    def inflateRequest = new InflateRequest(market)
    def inflationConfig = new InflationConfig(8, 9)


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

        def askTradeWindow = tradeWindow(askVolumeForTradeWndow, OrderType.ASK)
        def bidTradeWindow = tradeWindow(bidVolumeForTradeWndow, OrderType.BID)
        def tradeWindow = new TradeWindow(askTradeWindow, bidTradeWindow)

        tradeWindowService.fetchTradeWindow(market) >> Mono.just(tradeWindow)
        tradeWindowService.getMarketPrice(market) >> marketPrice

        def askedOrders = orders(askedOrderVolumes, OrderType.ASK)
        def biddenOrders = orders(biddenOrderVolumes, OrderType.BID)
        orderService.fetchAutomaticOrdersFor(market) >> Flux.fromIterable(askedOrders + biddenOrders)

        inflationConfigRepository.getConfigFor(market) >> inflationConfig

        inflationVolumeResolver.getInflationVolume(market, OrderType.ASK) >> askInflationVolume
        inflationVolumeResolver.getInflationVolume(market, OrderType.BID) >> bidInflationVolume

        def madeAskedPrices = priceUnitMultipliersForAskOrdersShouldBeMade.stream().map({ multiplier ->
            def price = marketPrice - (priceChangeUnit * multiplier)
            1 * orderService.requestOrder({
                it.symbol == symbol && it.orderType == OrderType.ASK \
                          && it.price == price && it.volume == askInflationVolume
            }) >> Mono.just(
                    new Order("${multiplier}", symbol, exchange, OrderType.ASK, price, askInflationVolume)
            )
            return price
        }).collect(Collectors.toList())

        def madeBidPrices = priceUnitMultipliersForBidOrdersShouldBeMade.stream().map({ multiplier ->
            def price = marketPrice + priceChangeUnit * multiplier
            1 * orderService.requestOrder({
                it.symbol == symbol && it.orderType == OrderType.BID \
                          && it.price == price && it.volume == bidInflationVolume
            }) >> Mono.just(
                    new Order("${multiplier}", symbol, exchange, OrderType.BID, price, bidInflationVolume)
            )
            return price
        }).collect(Collectors.toList())

        def cancelledAskedPrices = priceUnitMultipliersForAskOrdersShouldBeCanceled.stream().map({ int multiplier ->
            def price = marketPrice - (priceChangeUnit * multiplier)
            1 * orderService.cancelOrder({
                it.symbol == symbol && it.orderType == OrderType.ASK  \
                         && it.price == price
            }) >> Mono.just(
                    new Order("${multiplier}", symbol, exchange, OrderType.ASK, price, 1)
            )
            return price
        }).collect(Collectors.toList())

        def cancelledBiddenPrices = priceUnitMultipliersForBidOrdersShouldBeCanceled.stream().map({ int multiplier ->
            def price = marketPrice + priceChangeUnit * multiplier
            1 * orderService.cancelOrder({
                it.symbol == symbol && it.orderType == OrderType.BID  \
                         && it.price == price
            }) >> Mono.just(
                    new Order("${multiplier}", symbol, exchange, OrderType.BID, price, 1)
            )
            return price
        }).collect(Collectors.toList())

        expect:
        StepVerifier.create(sut.inflateTrades(inflateRequest))
                .assertNext({
                    it.getCreatedAskOrderPrices() == madeAskedPrices \
                       && it.getCreatedBidOrderPrices() == madeBidPrices \
                       && it.getCancelledAskOrderPrices() == cancelledAskedPrices \
                       && it.getCancelledBidOrderPrices() == cancelledBiddenPrices

                    println("Case: " + comment)
                    println("Ask Orders should be made" + madeAskedPrices)
                    println("Ask Orders actually made: " + it.createdAskOrderPrices)

                    println("Bid Orders should be made" + madeBidPrices)
                    println("Bid Orders actually made" + it.createdBidOrderPrices)


                    println("Ask Orders should be cancelled" + cancelledAskedPrices)
                    println("Ask Orders actually cancelled: " + it.cancelledAskOrderPrices)

                    println("Bid Orders should be cancelled" + cancelledBiddenPrices)
                    println("Bid Orders actually cancelled" + it.cancelledBidOrderPrices)
                })
                .verifyComplete()


        where:
        askVolumeForTradeWndow << [
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

        ]
        bidVolumeForTradeWndow << [
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
                []
        ]
        priceUnitMultipliersForAskOrdersShouldBeMade << [
                [1, 2, 3, 4, 5, 6, 7, 8],
                [6, 7],
                [6, 7],
                [6, 7],
                [],
                [],
                [],
                [],
                [],
                [],
                [6, 7],
                [6, 7],
                [6, 7],
        ]
        priceUnitMultipliersForBidOrdersShouldBeMade << [
                [1, 2, 3, 4, 5, 6, 7, 8, 9],
                [7, 8, 9],
                [7, 8, 9],
                [7, 8, 9],
                [],
                [],
                [],
                [7, 8, 9],
                [7, 8, 9],
                [7, 8, 9],
                [],
                [],
                []
        ]
        priceUnitMultipliersForAskOrdersShouldBeCanceled << [
                [],
                [],
                [],
                [],
                [8, 9],
                [8, 9],
                [],
                [8, 9],
                [8, 9],
                [],
                [],
                [],
                []
        ]
        priceUnitMultipliersForBidOrdersShouldBeCanceled << [
                [],
                [],
                [],
                [],
                [10, 11],
                [10, 11],
                [],
                [],
                [],
                [],
                [10, 11],
                [10, 11],
                []
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
        ]

    }

    List<TradePrice> tradeWindow(List<Integer> volumes, OrderType orderType) {
        return (0..<volumes.size()).stream()
                .filter({ index ->
                    volumes[index] != 0
                })
                .map({ index ->
                    def priceMultiplier = orderType == OrderType.BID ? index + 1 : -index
                    def volume = volumes[index]

                    BigDecimal price = marketPrice + priceChangeUnit * priceMultiplier
                    return new TradePrice(orderType, price, volume)
                })
                .collect(Collectors.toList())
    }

    List<Order> orders(List<Integer> volumes, OrderType orderType) {
        return (0..<volumes.size()).stream()
                .filter({ index ->
                    volumes[index] != 0
                })
                .map({ index ->
                    def priceMultiplier = orderType == OrderType.BID ? index + 1 : -index
                    def volume = volumes[index]
                    BigDecimal price = marketPrice + priceChangeUnit * priceMultiplier
                    return new Order("${index}", symbol, exchange, orderType, price, volume)
                })
                .collect(Collectors.toList())
    }
}
