package com.moebius.entropy.assembler

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.dto.view.AutomaticOrderForm
import spock.lang.Specification

class AutomaticOrderViewAssemblerTestSpec extends Specification {
    def market = new Market(Exchange.BOBOO, "GTAX2USDT", TradeCurrency.USDT, 2, 2)
    def sut = new AutomaticOrderViewAssembler()

    def "Assemble InflationConfig"() {
        given:
        def form = Mock(AutomaticOrderForm)
        form.getSellInflationCount() >> 1L
        form.getSellVolumeRangeFrom() >> BigDecimal.valueOf(12.34)
        form.getSellVolumeRangeTo() >> BigDecimal.valueOf(45.67)

        form.getBuyInflationCount() >> 2L
        form.getBuyVolumeRangeFrom() >> BigDecimal.valueOf(34.56)
        form.getBuyVolumeRangeTo() >> BigDecimal.valueOf(78.91)

        when:
        def inflationConfig = sut.assembleInitialInflationConfig(form)

        then:
        inflationConfig.askCount == form.getSellInflationCount().toInteger()
        inflationConfig.bidCount == form.getBuyInflationCount().toInteger()
        inflationConfig.getAskMinVolume() == form.getSellVolumeRangeFrom()
        inflationConfig.getAskMaxVolume() == form.getSellVolumeRangeTo()
    }

    def "Assemble DividedDummyOrderDto"() {
        given:
        def form = Mock(AutomaticOrderForm)
        form.getSellInflationCount() >> 1L
        form.getSellVolumeRangeFrom() >> BigDecimal.valueOf(12.34)
        form.getSellVolumeRangeTo() >> BigDecimal.valueOf(45.67)
        form.getSellDivisionFrom() >> BigDecimal.valueOf(78.89)
        form.getSellDivisionTo() >> BigDecimal.valueOf(91.12)
        form.getSellDivisionInterval() >> BigDecimal.valueOf(1.2)
        form.getSellDivisionTimeFrom() >> 10L
        form.getSellDivisionTimeTo() >> 20L

        form.getBuyInflationCount() >> 2L
        form.getBuyVolumeRangeFrom() >> BigDecimal.valueOf(34.56)
        form.getBuyVolumeRangeTo() >> BigDecimal.valueOf(78.91)
        form.getBuyDivisionFrom() >> BigDecimal.valueOf(120.34)
        form.getBuyDivisionTo() >> BigDecimal.valueOf(34.56)
        form.getBuyDivisionInterval() >> BigDecimal.valueOf(3.4)
        form.getBuyDivisionTimeFrom() >> 30L
        form.getBuyDivisionTimeTo() >> 40L

        when:
        def dividedDummyOrder = sut.assembleDivideDummyOrder(market, form)
        def inflationConfig = dividedDummyOrder.inflationConfig
        def askOrderConfig = dividedDummyOrder.askOrderConfig
        def bidOrderConfig = dividedDummyOrder.bidOrderConfig
        def marketDto = dividedDummyOrder.market

        then:
        inflationConfig.askCount == form.getSellInflationCount().toInteger()
        inflationConfig.bidCount == form.getBuyInflationCount().toInteger()
        inflationConfig.getAskMinVolume() == form.getSellVolumeRangeFrom()
        inflationConfig.getAskMaxVolume() == form.getSellVolumeRangeTo()

        askOrderConfig.getMinDividedOrderCount() == form.getSellDivisionTimeFrom().toInteger()
        askOrderConfig.getMaxDividedOrderCount() == form.getSellDivisionTimeTo().toInteger()
        askOrderConfig.getPeriod() == form.getSellDivisionInterval().floatValue()
        askOrderConfig.minReorderCount == form.getSellDivisionFrom()
        askOrderConfig.maxReorderCount == form.getSellDivisionTo()

        bidOrderConfig.getMinDividedOrderCount() == form.getBuyDivisionTimeFrom().toInteger()
        bidOrderConfig.getMaxDividedOrderCount() == form.getBuyDivisionTimeTo().toInteger()
        bidOrderConfig.getPeriod() == form.getBuyDivisionInterval().floatValue()
        bidOrderConfig.minReorderCount == form.getBuyDivisionFrom()
        bidOrderConfig.maxReorderCount == form.getBuyDivisionTo()

        marketDto.tradeCurrency == market.tradeCurrency
        marketDto.symbol == market.symbol
        marketDto.exchange == market.exchange
    }

    def "Assemble AutomaticOrderResult"() {
        given:
        def disposableIds = ["test-disposable-id", "test-disposable-id2"]

        when:
        def result = sut.assembleAutomaticOrderResult(disposableIds)

        then:
        result.disposableIds == disposableIds
    }
}
