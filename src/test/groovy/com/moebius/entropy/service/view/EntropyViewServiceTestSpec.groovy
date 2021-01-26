package com.moebius.entropy.service.view

import com.moebius.entropy.assembler.AutomaticOrderViewAssembler
import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.inflate.InflationConfig
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.dto.order.DividedDummyOrderDto
import com.moebius.entropy.dto.view.AutomaticOrderForm
import com.moebius.entropy.dto.view.AutomaticOrderResult
import com.moebius.entropy.repository.InflationConfigRepository
import com.moebius.entropy.service.order.boboo.BobooDividedDummyOrderService
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification

class EntropyViewServiceTestSpec extends Specification {
    def market = new Market(Exchange.BOBOO, "GTAX", TradeCurrency.USDT)
    def disposableId = "test-disposable-id"
    def automaticOrderViewAssembler = Mock(AutomaticOrderViewAssembler)
    def dividedDummyOrderService = Mock(BobooDividedDummyOrderService)
    def inflationConfigRepository = Mock(InflationConfigRepository)
    //TBD for rest service
    def sut = new EntropyViewService(
            automaticOrderViewAssembler, dividedDummyOrderService, inflationConfigRepository
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
        StepVerifier.create(sut.startAutomaticOrder(orderForm))
                .assertNext({ it.disposableId == disposableId })
                .verifyComplete()


        //TBD unimplemented services
    }
}
