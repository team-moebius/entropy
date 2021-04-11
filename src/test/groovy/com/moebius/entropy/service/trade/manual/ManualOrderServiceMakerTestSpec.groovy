package com.moebius.entropy.service.trade.manual


import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.ManualOrderMakingRequest
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.order.Order
import com.moebius.entropy.domain.order.OrderPosition
import com.moebius.entropy.domain.order.OrderRequest
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.repository.TradeWindowRepository
import com.moebius.entropy.service.order.OrderService
import com.moebius.entropy.service.order.boboo.ManualOrderMakerService
import com.moebius.entropy.util.EntropyRandomUtils
import org.apache.commons.lang3.tuple.Pair
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.util.stream.Collectors

@SuppressWarnings('GroovyAssignabilityCheck')
class ManualOrderServiceMakerTestSpec extends Specification {
    def randomUtil = Mock(EntropyRandomUtils)
    def orderService = Mock(OrderService)
    def tradeWindowRepository = Mock(TradeWindowRepository)

    @Subject
    def sut = new ManualOrderMakerService(randomUtil, orderService, tradeWindowRepository)
    @Shared
    def symbol = "GTAX2USDT"
    @Shared
    def exchange = Exchange.BOBOO
    def market = new Market(exchange, symbol, TradeCurrency.USDT, 2, 2)


    @Unroll
    def "Make #orderPosition Order with given condition"() {
        given:
        def randomVolumes = randomValues.stream()
                .map({ BigDecimal.valueOf(it) })
                .collect(Collectors.toList())

        def remainVolumes = zipCollections(randomVolumes, executedVolumes).stream()
                .map({
                    def volume = it.getLeft() as BigDecimal
                    def executedVolume = it.getRight() as float
                    return volume.subtract(BigDecimal.valueOf(executedVolume)).round(2)
                })
                .collect(Collectors.toList())
        def cancelledVolumes = remainVolumes.stream()
                .filter({ it.doubleValue() > 0.0 })
                .map({ it.round(2) })
                .collect(Collectors.toList())

        randomUtil.getRandomDecimal(reqVolumeFrom.floatValue(), reqVolumeTo.floatValue(), _) >> requestedVolume
        randomUtil.getRandomSlices(requestedVolume, selectedDivision, _) >> randomVolumes
        randomUtil.getRandomInteger(divisionRange[0], divisionRange[1]) >> selectedDivision
        tradeWindowRepository.getMarketPriceForSymbol(market) >> marketPrice
        def requestedMarketPrice = OrderPosition.ASK == orderPosition ? marketPrice.subtract(market.tradeCurrency.priceUnit) : marketPrice

        (0..<selectedDivision).forEach({ index ->
            def volume = randomVolumes[index]
            def remainVolume = remainVolumes[index]
            orderService.requestManualOrder({
                it.market == market && it.orderPosition == orderPosition && it.price == BigDecimal.valueOf(requestedMarketPrice) && it.volume == volume
            } as OrderRequest) >> Mono.just(new Order("$index", market, orderPosition, requestedMarketPrice, remainVolume))
            if (remainVolume.doubleValue() > 0.0) {
                orderService.cancelOrder({
                    it.market == market && it.orderPosition == orderPosition && BigDecimal.valueOf(requestedMarketPrice) && it.volume == remainVolume
                } as Order) >> Mono.just(new Order("$index", market, orderPosition, requestedMarketPrice, remainVolume))
            }
        })
        def request = ManualOrderMakingRequest.builder()
                .orderPosition(orderPosition)
                .startRange(divisionRange[0])
                .endRange(divisionRange[1])
                .requestedVolumeFrom(BigDecimal.valueOf(reqVolumeFrom))
                .requestedVolumeTo(BigDecimal.valueOf(reqVolumeTo))
                .market(market)
                .build()

        expect:
        StepVerifier.create(sut.requestManualOrderMaking(request))
                .assertNext({
                    def requestedOrders = it.getRequestedOrders()
                    assert requestedOrders.size() == selectedDivision
                    def expectedPrice = requestedMarketPrice
                    zipCollections(requestedOrders, remainVolumes).forEach({
                        def order = it.getLeft() as Order
                        def remainVolume = BigDecimal.valueOf(it.getRight() as float)
                        assert order.orderPosition == orderPosition
                        assert order.market == market
                        assert order.volume == remainVolume
                        assert order.price == expectedPrice
                    })
                    def cancelledOrders = it.getCancelledOrders()
                    assert cancelledOrders.size() == cancelledVolumes.size()
                    zipCollections(cancelledOrders, cancelledVolumes).forEach({
                        def order = it.getLeft() as Order
                        def cancelledVolume = it.getRight()
                        assert order.orderPosition == orderPosition
                        assert order.market == market
                        assert order.volume == cancelledVolume
                        assert order.price == expectedPrice
                    })
                })
                .verifyComplete()


        where:
        orderPosition     | divisionRange | selectedDivision | reqVolumeFrom | reqVolumeTo | requestedVolume | marketPrice | currentVolume | executedVolumes                                                           | randomValues
        OrderPosition.ASK | [1, 10]       | 10               | 2000.0        | 3000.0      | 2500.0          | 17.52       | 2500.0        | [177.0, 175.8, 320.5, 198.6, 275.2, 252.12, 193.0, 364.0, 275.12, 268.66] | [177.0, 175.8, 320.5, 198.6, 275.2, 252.12, 193.0, 364.0, 275.12, 268.66]
        OrderPosition.ASK | [1, 10]       | 1                | 2000.0        | 3500.0      | 3000.0          | 17.52       | 2500.0        | [2500.0]                                                                  | [3000.0]
        OrderPosition.ASK | [1, 10]       | 1                | 1500.0        | 2500.0      | 2000.0          | 17.52       | 2500.0        | [2000.0]                                                                  | [2000.0]
        OrderPosition.ASK | [1, 10]       | 1                | 2000.0        | 3000.0      | 2500.0          | 17.52       | 2500.0        | [2500.0]                                                                  | [2500.0]
        OrderPosition.ASK | [1, 10]       | 5                | 2000.0        | 3000.0      | 2500.0          | 17.52       | 2500.0        | [312.02, 754.12, 125.23, 867.0, 441.63]                                   | [312.02, 754.12, 125.23, 867.0, 441.63]
        OrderPosition.ASK | [1, 10]       | 5                | 2500.0        | 3500.0      | 3000.0          | 17.52       | 2500.0        | [312.02, 754.12, 125.23, 867.0, 441.63]                                   | [312.02, 754.12, 125.23, 867.0, 941.63]
        OrderPosition.ASK | [1, 10]       | 5                | 1500.0        | 2500.0      | 2000.0          | 17.52       | 2500.0        | [312.02, 454.12, 125.23, 867.0, 58.37]                                    | [312.02, 454.12, 125.23, 867.0, 58.37]
        OrderPosition.BID | [1, 10]       | 10               | 2000.0        | 3000.0      | 2500.0          | 17.52       | 2500.0        | [177.0, 175.8, 320.5, 198.6, 275.2, 252.12, 193.0, 364.0, 275.12, 268.66] | [177.0, 175.8, 320.5, 198.6, 275.2, 252.12, 193.0, 364.0, 275.12, 268.66]
        OrderPosition.BID | [1, 10]       | 1                | 2000.0        | 3000.0      | 2500.0          | 17.52       | 2500.0        | [2500.0]                                                                  | [3000.0]
        OrderPosition.BID | [1, 10]       | 1                | 2500.0        | 3500.0      | 3000.0          | 17.52       | 2500.0        | [2000.0]                                                                  | [2000.0]
        OrderPosition.BID | [1, 10]       | 1                | 1500.0        | 2500.0      | 2000.0          | 17.52       | 2500.0        | [2500.0]                                                                  | [2500.0]
        OrderPosition.BID | [1, 10]       | 5                | 2000.0        | 3000.0      | 2500.0          | 17.52       | 2500.0        | [312.02, 754.12, 125.23, 867.0, 441.63]                                   | [312.02, 754.12, 125.23, 867.0, 441.63]
        OrderPosition.BID | [1, 10]       | 5                | 2500.0        | 3500.0      | 3000.0          | 17.52       | 2500.0        | [312.02, 754.12, 125.23, 867.0, 441.63]                                   | [312.02, 754.12, 125.23, 867.0, 941.63]
        OrderPosition.BID | [1, 10]       | 5                | 1500.0        | 2500.0      | 2000.0          | 17.52       | 2500.0        | [312.02, 454.12, 125.23, 867.0, 58.37]                                    | [312.02, 454.12, 125.23, 867.0, 58.37]
    }

    static List<Pair<?, ?>> zipCollections(List<?> first, List<?> second) {
        assert first.size() == second.size()
        (0..<first.size()).stream().map({ index ->
            Pair.of(first.get(index), second.get(index))
        }).collect(Collectors.toList())
    }

}




