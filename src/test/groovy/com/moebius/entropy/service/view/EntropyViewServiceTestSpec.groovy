package com.moebius.entropy.service.view

import com.moebius.entropy.assembler.AutomaticOrderViewAssembler
import com.moebius.entropy.assembler.ManualOrderRequestAssembler
import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.ManualOrderMakingRequest
import com.moebius.entropy.domain.ManualOrderResult
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.inflate.InflationConfig
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.dto.order.DividedDummyOrderDto
import com.moebius.entropy.dto.view.AutomaticOrderCancelForm
import com.moebius.entropy.dto.view.AutomaticOrderForm
import com.moebius.entropy.dto.view.AutomaticOrderResult
import com.moebius.entropy.dto.view.ManualOrderForm
import com.moebius.entropy.repository.DisposableOrderRepository
import com.moebius.entropy.repository.InflationConfigRepository
import com.moebius.entropy.service.order.boboo.BobooDividedDummyOrderService
import com.moebius.entropy.service.order.boboo.BobooOrderService
import com.moebius.entropy.service.trade.manual.ManualOrderMakerService
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification

import java.time.Duration

class EntropyViewServiceTestSpec extends Specification {
    def market = new Market(Exchange.BOBOO, "GTAXUSDT", TradeCurrency.USDT)
    def disposableId = "test-disposable-id"
    def automaticOrderViewAssembler = Mock(AutomaticOrderViewAssembler)
    def manualOrderViewAssembler = Mock(ManualOrderRequestAssembler)
    def manualOrderMakerService = Mock(ManualOrderMakerService)
    def dividedDummyOrderService = Mock(BobooDividedDummyOrderService)
    def inflationConfigRepository = Mock(InflationConfigRepository)
    def bobooOrderService = Mock(BobooOrderService)
    def disposableOrderRepository = Mock(DisposableOrderRepository)
    def tradeWindowQueryService = Mock(TradeWindowQueryService)

    //TBD for rest service
    def sut = new EntropyViewService(
            automaticOrderViewAssembler, dividedDummyOrderService, inflationConfigRepository,
            bobooOrderService, manualOrderViewAssembler, manualOrderMakerService, disposableOrderRepository,
            tradeWindowQueryService
    )


    def "Should request to start automatic orders from request form"() {
        given:
        def orderForm = Mock(AutomaticOrderForm)
        def mockInflationConfig = Mock(InflationConfig)
        def mockDividedDummyOrderDto = Mock(DividedDummyOrderDto)
        1 * automaticOrderViewAssembler.assembleInflationConfig(orderForm) >> mockInflationConfig
        1 * automaticOrderViewAssembler.assembleDivideDummyOrder(market, orderForm) >> mockDividedDummyOrderDto
        1 * automaticOrderViewAssembler.assembleAutomaticOrderResult(disposableId) >> AutomaticOrderResult.builder()
                .disposableId(disposableId)
                .build()
        1 * inflationConfigRepository.saveConfigFor(market, mockInflationConfig)
        1 * dividedDummyOrderService.executeDividedDummyOrders(mockDividedDummyOrderDto) >> Mono.just(
                ResponseEntity.ok(disposableId)
        )

        expect:
        StepVerifier.create(sut.startAutomaticOrder(market, orderForm))
                .assertNext({ it.disposableId == disposableId })
                .verifyComplete()


        //TBD unimplemented services
    }

    def "Should cancel ongoing automatic orders"() {
        given:
        def cancelForm = Mock(AutomaticOrderCancelForm)
        cancelForm.getMarket() >> market
        cancelForm.getDisposableId() >> disposableId
        disposableOrderRepository.getAll() >> [disposableId]
        1 * inflationConfigRepository.getConfigFor(market) >> InflationConfig.builder()
                .enable(true)
                .build()
        1 * inflationConfigRepository.saveConfigFor(market, { it.enable == false })
        1 * bobooOrderService.stopOrder(disposableId) >> Mono.just(ResponseEntity.ok(disposableId))

        expect:
        StepVerifier.create(sut.cancelAutomaticOrder(cancelForm))
                .assertNext({
                    it.cancelledDisposableIds[0] == disposableId && it.inflationCancelled
                })
                .verifyComplete()
    }

    def "Should request manual order"() {
        given:
        def orderForm = Mock(ManualOrderForm)
        def manualOrderRequest = Mock(ManualOrderMakingRequest)
        def manualOrderResult = Mock(ManualOrderResult)
        1 * manualOrderViewAssembler.assembleManualOrderRequest(market, orderForm) >> manualOrderRequest
        1 * manualOrderMakerService.requestManualOrderMaking(manualOrderRequest) >> Mono.just(
                manualOrderResult
        )

        expect:
        StepVerifier.create(sut.requestManualOrder(market, orderForm))
                .assertNext({ it instanceof ManualOrderResult })
                .verifyComplete()
    }

    def "Should get serious of market prices"() {
        given:
        def intervalSeconds = 1
        def prices = [
                12.23, 123.32, 232.12, 123.63, 123.64, 123.53, 643.32, 6544.33, 23.21, 12.46
        ]
        tradeWindowQueryService.getMarketPrice(market) >>> prices

        expect:
        StepVerifier.withVirtualTime({ sut.receiveMarketPriceDto(market, Duration.ofSeconds(intervalSeconds)).take(3) })
                .thenAwait(Duration.ofSeconds(intervalSeconds))
                .assertNext({
                    assert it.getExchange() == market.getExchange()
                    assert it.getTradeCurrency() == market.getTradeCurrency().name()
                    assert it.getPriceUnit() == market.getTradeCurrency().priceUnit
                    assert it.getSymbol() == "GTAX"
                    it.getPrice() == prices[0]
                })
                .thenAwait(Duration.ofSeconds(intervalSeconds))
                .assertNext({
                    assert it.getExchange() == market.getExchange()
                    assert it.getTradeCurrency() == market.getTradeCurrency().name()
                    assert it.getPriceUnit() == market.getTradeCurrency().priceUnit
                    assert it.getSymbol() == "GTAX"
                    it.getPrice() == prices[1]
                })
                .thenAwait(Duration.ofSeconds(intervalSeconds))
                .assertNext({
                    assert it.getExchange() == market.getExchange()
                    assert it.getTradeCurrency() == market.getTradeCurrency().name()
                    assert it.getPriceUnit() == market.getTradeCurrency().priceUnit
                    assert it.getSymbol() == "GTAX"
                    it.getPrice() == prices[2]
                })
                .verifyComplete()

    }
}
