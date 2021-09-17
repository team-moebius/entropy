package com.moebius.entropy.service.tradewindow

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.inflate.InflateRequest
import com.moebius.entropy.domain.inflate.InflationConfig
import com.moebius.entropy.domain.order.Order
import com.moebius.entropy.domain.order.OrderPosition
import com.moebius.entropy.domain.order.OrderRequest
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.domain.trade.TradePrice
import com.moebius.entropy.domain.trade.TradeWindow
import com.moebius.entropy.repository.InflationConfigRepository
import com.moebius.entropy.service.order.OrderService
import com.moebius.entropy.service.order.OrderServiceFactory
import com.moebius.entropy.util.EntropyRandomUtils
import com.moebius.entropy.util.SpreadWindowResolver
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.math.RoundingMode

class TradeWindowInflateServiceTestSpec extends Specification {
    @Shared
    def marketPrice = new BigDecimal("11.35")
    @Shared
    def symbol = "GTAX2USDT"
    @Shared
    def exchange = Exchange.BOBOO
    def tradeWindowQueryService = Mock(TradeWindowQueryService)
    def inflationConfigRepository = Mock(InflationConfigRepository)
    def orderService = Mock(OrderService)
    def orderServiceFactory = Mock(OrderServiceFactory)
    def inflationVolumeResolver = Mock(TradeWindowVolumeResolver)
    def randomUtil = Mock(EntropyRandomUtils)
    def spreadWindowResolver = new SpreadWindowResolver(randomUtil)


    @Subject
    TradeWindowInflateService sut = new TradeWindowInflateService(
            tradeWindowQueryService, inflationConfigRepository, orderServiceFactory,
            inflationVolumeResolver, spreadWindowResolver
    )
    def askInflationVolume = new BigDecimal("99.9999")
    def bidInflationVolume = new BigDecimal("111.1111")

    def targetMarket = new Market(exchange, symbol, TradeCurrency.DETAILED_USDT, 2, 2)
    def market = new Market(exchange, symbol, TradeCurrency.USDT, 2, 2)
    def inflateRequest = new InflateRequest(targetMarket)


//    1. Event를 Parameter로 받고(Event data에 Exchange와 Symbol 받음)
//    2. Trade Window 를 가져오고 여기서 개입이 필요한지 판단(매수 매도 각각 호가 수가 설정된 값 미만인지 판단)
//    3. 개입이 필요하다면 내 Order를 가져오고 거기에서 취소해야할 주문 선별
//    4. 3에서 선별된 주문을 취소
//    5. 설정된 호가 수를 채우기 위해 주문을 생성
    @Unroll
    def "Test with spreadWindow #spreadWindow when #comment"() {
        given:
        def inflationConfig = InflationConfig.builder()
                .askCount(askCount)
                .bidCount(bidCount)
                .bidMinVolume(bidInflationVolume)
                .askMinVolume(askInflationVolume)
                .market(market)
                .spreadWindow(spreadWindow)
                .bidShift(1)
                .askShift(1)
                .enable(true)
                .build()

        def askTradeWindow = (params.get("askTradeWindowPair") as List<List>)
                .collect {
                    String priceStr = it.get(0)
                    int volume = it.get(1) as Integer
                    new TradePrice(OrderPosition.ASK, new BigDecimal(priceStr), new BigDecimal(volume))
                }

        def bidTradeWindow = (params.get("bidTradeWindowPair") as List<List>)
                .collect {
                    String priceStr = it.get(0)
                    int volume = it.get(1) as Integer
                    new TradePrice(OrderPosition.BID, new BigDecimal(priceStr), new BigDecimal(volume))
                }

        def tradeWindow = new TradeWindow(askTradeWindow, bidTradeWindow)

        tradeWindowQueryService.getTradeWindowMono(market) >> Mono.just(tradeWindow)
        tradeWindowQueryService.getMarketPrice(market) >> marketPrice

        inflationConfigRepository.getConfigFor(targetMarket) >> inflationConfig

        inflationVolumeResolver.getInflationVolume(market, OrderPosition.ASK) >> askInflationVolume
        inflationVolumeResolver.getInflationVolume(market, OrderPosition.BID) >> bidInflationVolume
        orderServiceFactory.getOrderService(_ as Exchange) >> orderService
        orderService.cancelOrder(_ as Order) >> { Order it ->
            Mono.just(new Order(
                    "cancelled-${it.orderId}", it.market, it.orderPosition, it.price, it.volume
            ))
        }
        orderService.requestOrder(_ as OrderRequest) >> { OrderRequest it ->
            Mono.just(new Order(
                    "made-${it.volume}", it.market, it.orderPosition, it.price, it.volume
            ))
        }
        randomUtil.getRandomDecimal(_ as BigDecimal, _ as BigDecimal, _ as Integer) >> { BigDecimal min, BigDecimal max, int decimalPlaces ->
            return max.add(min).divide(BigDecimal.valueOf(2L)).setScale(decimalPlaces, RoundingMode.HALF_UP)
        }

        def madeAskedPrices = (params.get("madeAskPrices") as List<String>).collect { new BigDecimal(it) }
        def madeBidPrices = (params.get("madeBidPrices") as List<String>).collect { new BigDecimal(it) }

        def cancelledAskedPrices = (params.get("cancelledAskPrices") as List<String>).collect { new BigDecimal(it) }
        def askOrdersInExchange = cancelledAskedPrices.withIndex()
                .collect {
                    def (price, index) = it
                    return new Order("cancelled-${index + 1}", market, OrderPosition.ASK, price, askInflationVolume)
                }

        def cancelledBiddenPrices = (params.get("cancelledBidPrices") as List<String>).collect { new BigDecimal(it) }
        def bidOrdersInExchange = cancelledBiddenPrices.withIndex()
                .collect {
                    def (price, index) = it
                    return new Order("cancelled-${index}", market, OrderPosition.BID, price, bidInflationVolume)
                }

        orderService.fetchAllOrdersFor(market) >> Flux.fromIterable(
                bidOrdersInExchange + askOrdersInExchange
        )

        expect:
        StepVerifier.create(sut.inflateOrders(inflateRequest))
                .recordWith({ return [] })
                .thenConsumeWhile({ return true })
                .consumeRecordedWith({ allOrders ->
                    def madeAskOrderActual = allOrders.stream()
                            .filter { order ->
                                order.getOrderId().startsWith("made-") \
                                  && order.orderPosition == OrderPosition.ASK
                            }
                            .collect { it }

                    def madeBidOrderActual = allOrders.stream()
                            .filter { order ->
                                order.getOrderId().startsWith("made-") \
                                  && order.orderPosition == OrderPosition.BID
                            }
                            .collect { it }

                    def cancelledAskOrderActual = allOrders.stream()
                            .filter { order ->
                                order.getOrderId().startsWith("cancelled-") \
                                  && order.orderPosition == OrderPosition.ASK
                            }
                            .collect { it }

                    def cancelledBidOrderActual = allOrders.stream()
                            .filter { order ->
                                order.getOrderId().startsWith("cancelled-") \
                                  && order.orderPosition == OrderPosition.BID
                            }
                            .collect { it }

                    assert madeAskOrderActual.size() == madeAskedPrices.size()
                    assert madeAskOrderActual.every { madeAskedPrices.contains(it.price) }

                    assert madeBidOrderActual.size() == madeBidPrices.size()
                    assert madeBidOrderActual.every { madeBidPrices.contains(it.price) }

                    assert cancelledAskOrderActual.size() == cancelledAskedPrices.size()
                    assert cancelledAskOrderActual.every { cancelledAskedPrices.contains(it.price) }

                    assert cancelledBidOrderActual.size() == cancelledBiddenPrices.size()
                    assert cancelledBidOrderActual.every { cancelledBiddenPrices.contains(it.price) }
                })
                .verifyComplete()


        where:
        spreadWindow | askCount | bidCount | comment
        1            | 8        | 9        | "trade window is empty on first start"
        1            | 8        | 9        | "both trade window count less than objective count"
        1            | 8        | 9        | "both trade window count larger than objective count"
        1            | 8        | 9        | "both trade window count larger than objective count without no automatic orders"
        1            | 8        | 9        | "ask count over and bid count less than objective"
        1            | 8        | 9        | "ask count over and bid count less than objective without no automatic orders"
        1            | 8        | 9        | "ask count less and bid count over than objective"
        1            | 8        | 9        | "ask count less and bid count over than objective without no automatic orders"
        1            | 8        | 9        | "both trade window count larger than objective count but there's some missed price"

        5            | 8        | 9        | "trade window is empty on first start"
        5            | 8        | 9        | "both trade window count less than objective count"
        5            | 8        | 9        | "both trade window count larger than objective count"
        5            | 8        | 9        | "both trade window count larger than objective count without no automatic orders"
        5            | 8        | 9        | "ask count over and bid count less than objective"
        5            | 8        | 9        | "ask count over and bid count less than objective without no automatic orders"
        5            | 8        | 9        | "ask count less and bid count over than objective"
        5            | 8        | 9        | "ask count less and bid count over than objective without no automatic orders"
        5            | 8        | 9        | "both trade window count larger than objective count but there's some missed price"

        //MarketPrice = 11.35
        //UnitPrice = 0.01
        params << [
                [
                        "askTradeWindowPair": [],
                        "bidTradeWindowPair": [],
                        "madeAskPrices"     : ["11.37", "11.38", "11.39", "11.40", "11.41", "11.42", "11.43", "11.44"],
                        "madeBidPrices"     : ["11.33", "11.32", "11.31", "11.30", "11.29", "11.28", "11.27", "11.26", "11.25"],
                        "cancelledAskPrices": [],
                        "cancelledBidPrices": [],
                        "comment"           : "trade window is empty on first start"
                ],
                [

                        //Starting from 1 unit price more than market price
                        "askTradeWindowPair": [["11.36", 123], ["11.37", 124], ["11.38", 125], ["11.39", 126], ["11.40", 127], ["11.41", 128]],
                        "bidTradeWindowPair": [["11.34", 200], ["11.33", 201], ["11.32", 202], ["11.31", 203], ["11.30", 204], ["11.29", 205], ["11.28", 206]],
                        "madeAskPrices"     : ["11.42", "11.43", "11.44"],
                        "madeBidPrices"     : ["11.27", "11.26", "11.25"],
                        "cancelledAskPrices": ["11.36"],
                        "cancelledBidPrices": ["11.34"],
                        "comment"           : "both trade window count less than objective count",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.37", 124], ["11.38", 125], ["11.39", 126], ["11.40", 127], ["11.41", 128], ["11.42", 129], ["11.43", 130], ["11.44", 131], ["11.45", 132]],
                        "bidTradeWindowPair": [["11.34", 200], ["11.33", 201], ["11.32", 202], ["11.31", 203], ["11.30", 204], ["11.29", 205], ["11.28", 206], ["11.27", 207], ["11.26", 208], ["11.25", 209], ["11.24", 210], ["11.23", 211]],
                        "madeAskPrices"     : [],
                        "madeBidPrices"     : [],
                        "cancelledAskPrices": ["11.36", "11.45"],
                        "cancelledBidPrices": ["11.34", "11.23"],
                        "comment"           : "both trade window count larger than objective count",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.37", 124], ["11.38", 125], ["11.39", 126], ["11.40", 127], ["11.41", 128], ["11.42", 129], ["11.43", 130], ["11.44", 131], ["11.45", 132]],
                        "bidTradeWindowPair": [["11.34", 200], ["11.33", 201], ["11.32", 202], ["11.31", 203], ["11.30", 204], ["11.29", 205], ["11.28", 206], ["11.27", 207], ["11.26", 208], ["11.25", 209], ["11.24", 210], ["11.23", 211]],
                        "madeAskPrices"     : [],
                        "madeBidPrices"     : [],
                        "cancelledAskPrices": ["11.36"],
                        "cancelledBidPrices": ["11.34"],
                        "comment"           : "both trade window count larger than objective count without no automatic orders",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.37", 124], ["11.38", 125], ["11.39", 126], ["11.40", 127], ["11.41", 128], ["11.42", 129], ["11.43", 130], ["11.44", 131], ["11.45", 132]],
                        "bidTradeWindowPair": [["11.34", 200], ["11.33", 201], ["11.32", 202], ["11.31", 203], ["11.30", 204], ["11.29", 205], ["11.28", 206]],
                        "madeAskPrices"     : [],
                        "madeBidPrices"     : ["11.27", "11.26", "11.25"],
                        "cancelledAskPrices": ["11.36", "11.45"],
                        "cancelledBidPrices": ["11.34"],
                        "comment"           : "ask count over and bid count less than objective",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.37", 124], ["11.38", 125], ["11.39", 126], ["11.40", 127], ["11.41", 128], ["11.42", 129], ["11.43", 130], ["11.44", 131], ["11.45", 132]],
                        "bidTradeWindowPair": [["11.34", 200], ["11.33", 201], ["11.32", 202], ["11.31", 203], ["11.30", 204], ["11.29", 205], ["11.28", 206]],
                        "madeAskPrices"     : [],
                        "madeBidPrices"     : ["11.27", "11.26", "11.25"],
                        "cancelledAskPrices": ["11.36"],
                        "cancelledBidPrices": ["11.34"],
                        "comment"           : "ask count over and bid count less than objective without no automatic orders",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.37", 124], ["11.38", 125], ["11.39", 126], ["11.40", 127], ["11.41", 128]],
                        "bidTradeWindowPair": [["11.34", 200], ["11.33", 201], ["11.32", 202], ["11.31", 203], ["11.30", 204], ["11.29", 205], ["11.28", 206], ["11.27", 207], ["11.26", 208], ["11.25", 209], ["11.24", 210], ["11.23", 211]],
                        "madeAskPrices"     : ["11.42", "11.43", "11.44"],
                        "madeBidPrices"     : [],
                        "cancelledAskPrices": ["11.36"],
                        "cancelledBidPrices": ["11.34", "11.23"],
                        "comment"           : "ask count less and bid count over than objective",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.37", 124], ["11.38", 125], ["11.39", 126], ["11.40", 127], ["11.41", 128]],
                        "bidTradeWindowPair": [["11.34", 200], ["11.33", 201], ["11.32", 202], ["11.31", 203], ["11.30", 204], ["11.29", 205], ["11.28", 206], ["11.27", 207], ["11.26", 208], ["11.25", 209], ["11.24", 210], ["11.23", 211]],
                        "madeAskPrices"     : ["11.42", "11.43", "11.44"],
                        "madeBidPrices"     : [],
                        "cancelledAskPrices": ["11.36"],
                        "cancelledBidPrices": ["11.34"],
                        "comment"           : "ask count less and bid count over than objective without no automatic orders",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.37", 000], ["11.38", 125], ["11.39", 126], ["11.40", 127], ["11.41", 128], ["11.42", 129], ["11.43", 130], ["11.44", 131], ["11.45", 132]],
                        "bidTradeWindowPair": [["11.34", 200], ["11.33", 201], ["11.32", 202], ["11.31", 000], ["11.30", 204], ["11.29", 205], ["11.28", 206], ["11.27", 207], ["11.26", 208], ["11.25", 209], ["11.24", 210], ["11.23", 211]],
                        "madeAskPrices"     : ["11.37"],
                        "madeBidPrices"     : ["11.31"],
                        "cancelledAskPrices": ["11.36", "11.45"],
                        "cancelledBidPrices": ["11.34", "11.23"],
                        "comment"           : "both trade window count larger than objective count but there's some missed price",
                ],



                //(11.36+11.41)/2 == 11.39
                //(11.34+11.29)/2 == 11.30
                [
                        "askTradeWindowPair": [],
                        "bidTradeWindowPair": [],
                        "madeAskPrices"     : ["11.44", "11.49", "11.54", "11.59", "11.64", "11.69", "11.74", "11.79"],
                        "madeBidPrices"     : ["11.27", "11.22", "11.17", "11.12", "11.07", "11.02", "10.97", "10.92", "10.87"],
                        "cancelledAskPrices": [],
                        "cancelledBidPrices": [],
                        "comment"           : "trade window is empty on first start"
                ],
                [
                        //11.36, 11.41, 11.46, 11.51, 11.56, 11.61, 11.66, 11.71, 11.76, 11.81, 11.86, 11.91, 11.96, 12.01,
                        //11.34, 11.29, 11.24, 11.19, 11.14, 11.09, 11.04, 10.99, 10.94, 10.89, 10.84, 10.79, 10.74, 10.69, 10.64, 10.59
                        "askTradeWindowPair": [["11.36", 123], ["11.39", 123], ["11.43", 124], ["11.47", 125], ["11.55", 126], ["11.56", 127], ["11.61", 128]],
                        "bidTradeWindowPair": [["11.34", 201], ["11.29", 201], ["11.25", 202], ["11.20", 203], ["11.18", 204], ["11.10", 205], ["11.08", 206]],
                        "madeAskPrices"     : ["11.69", "11.74", "11.79"],
                        "madeBidPrices"     : ["11.02", "10.97", "10.92", "10.87"],
                        "cancelledAskPrices": ["11.36"],
                        "cancelledBidPrices": ["11.34"],
                        "comment"           : "both trade window count less than objective count",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.39", 123], ["11.43", 124], ["11.47", 125], ["11.55", 126], ["11.56", 127], ["11.61", 128], ["11.70", 128], ["11.72", 129], ["11.77", 135], ["11.78", 130], ["11.85", 131]],
                        "bidTradeWindowPair": [["11.33", 201], ["11.29", 201], ["11.25", 202], ["11.20", 203], ["11.18", 204], ["11.10", 205], ["11.08", 206], ["11.02", 207], ["10.97", 208], ["10.92", 209], ["10.87", 222], ["10.86", 210]],
                        "madeAskPrices"     : [],
                        "madeBidPrices"     : [],
                        "cancelledAskPrices": ["11.36", "11.78"],
                        "cancelledBidPrices": ["11.33", "10.86"],
                        "comment"           : "both trade window count larger than objective count",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.39", 123], ["11.43", 124], ["11.47", 125], ["11.55", 126], ["11.56", 127], ["11.61", 128], ["11.70", 128], ["11.72", 129], ["11.77", 135], ["11.78", 130], ["11.85", 131]],
                        "bidTradeWindowPair": [["11.33", 201], ["11.29", 201], ["11.25", 202], ["11.20", 203], ["11.18", 204], ["11.10", 205], ["11.08", 206], ["11.02", 207], ["10.97", 208], ["10.92", 209], ["10.87", 222], ["10.86", 210]],
                        "madeAskPrices"     : [],
                        "madeBidPrices"     : [],
                        "cancelledAskPrices": ["11.36"],
                        "cancelledBidPrices": ["11.34"],
                        "comment"           : "both trade window count larger than objective count without no automatic orders",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.39", 123], ["11.43", 124], ["11.47", 125], ["11.55", 126], ["11.56", 127], ["11.61", 128], ["11.70", 128], ["11.72", 129], ["11.77", 135], ["11.78", 130], ["11.85", 131]],
                        "bidTradeWindowPair": [["11.33", 201], ["11.29", 201], ["11.25", 202], ["11.20", 203], ["11.18", 204], ["11.10", 205], ["11.08", 206]],
                        "madeAskPrices"     : [],
                        "madeBidPrices"     : ["11.02", "10.97", "10.92", "10.87"],
                        "cancelledAskPrices": ["11.36", "11.78"],
                        "cancelledBidPrices": ["11.34"],
                        "comment"           : "ask count over and bid count less than objective",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.39", 123], ["11.43", 124], ["11.47", 125], ["11.55", 126], ["11.56", 127], ["11.61", 128], ["11.70", 128], ["11.72", 129], ["11.77", 135], ["11.78", 130], ["11.85", 131]],
                        "bidTradeWindowPair": [["11.33", 201], ["11.29", 201], ["11.25", 202], ["11.20", 203], ["11.18", 204], ["11.10", 205], ["11.08", 206]],
                        "madeAskPrices"     : [],
                        "madeBidPrices"     : ["11.02", "10.97", "10.92", "10.87"],
                        "cancelledAskPrices": ["11.36"],
                        "cancelledBidPrices": ["11.34"],
                        "comment"           : "ask count over and bid count less than objective without no automatic orders",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.39", 123], ["11.43", 124], ["11.47", 125], ["11.55", 126], ["11.56", 127], ["11.61", 128]],
                        "bidTradeWindowPair": [["11.33", 201], ["11.29", 201], ["11.25", 202], ["11.20", 203], ["11.18", 204], ["11.10", 205], ["11.08", 206], ["11.02", 207], ["10.97", 208], ["10.92", 209], ["10.87", 222], ["10.86", 210]],
                        "madeAskPrices"     : ["11.69", "11.74", "11.79"],
                        "madeBidPrices"     : [],
                        "cancelledAskPrices": ["11.36"],
                        "cancelledBidPrices": ["11.33", "10.86"],
                        "comment"           : "ask count less and bid count over than objective",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.39", 123], ["11.43", 124], ["11.47", 125], ["11.55", 126], ["11.56", 127], ["11.61", 128]],
                        "bidTradeWindowPair": [["11.33", 201], ["11.29", 201], ["11.25", 202], ["11.20", 203], ["11.18", 204], ["11.10", 205], ["11.08", 206], ["11.02", 207], ["10.97", 208], ["10.92", 209], ["10.87", 222], ["10.86", 210]],
                        "madeAskPrices"     : ["11.69", "11.74", "11.79"],
                        "madeBidPrices"     : [],
                        "cancelledAskPrices": ["11.36"],
                        "cancelledBidPrices": ["11.34"],
                        "comment"           : "ask count less and bid count over than objective without no automatic orders",
                ],
                [
                        "askTradeWindowPair": [["11.36", 123], ["11.39", 123], ["11.43", 124], ["11.47", 000], ["11.55", 126], ["11.56", 127], ["11.61", 128], ["11.70", 128], ["11.72", 129], ["11.77", 135], ["11.78", 130], ["11.85", 131]],
                        "bidTradeWindowPair": [["11.34", 201], ["11.30", 201], ["11.25", 202], ["11.20", 000], ["11.18", 204], ["11.10", 205], ["11.08", 206], ["11.02", 207], ["10.97", 208], ["10.92", 209], ["10.87", 222], ["10.86", 210]],
                        "madeAskPrices"     : ["11.49"],
                        "madeBidPrices"     : ["11.22"],
                        "cancelledAskPrices": ["11.36", "11.78"],
                        "cancelledBidPrices": ["11.33", "10.86"],
                        "comment"           : "both trade window count larger than objective count but there's some missed price",
                ]
        ]
    }
}
