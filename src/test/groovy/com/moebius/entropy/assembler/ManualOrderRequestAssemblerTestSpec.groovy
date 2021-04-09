package com.moebius.entropy.assembler

import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.order.OrderPosition
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.dto.view.ManualOrderForm
import spock.lang.Specification

class ManualOrderRequestAssemblerTestSpec extends Specification {
    def sut = new ManualOrderRequestAssembler()
    def market = new Market(Exchange.BOBOO, "GTAX2USDT", TradeCurrency.USDT, 1)

    def "Should assemble manual order request"() {
        given:
        def manualOrderForm = Mock(ManualOrderForm)
        manualOrderForm.getOrderPosition() >> ORDER_POSITION_STRING
        manualOrderForm.getManualVolumeRangeFrom() >> BigDecimal.valueOf(123.45)
        manualOrderForm.getManualVolumeRangeTo() >> BigDecimal.valueOf(4567.12)
        manualOrderForm.getManualVolumeDivisionFrom() >> 12L
        manualOrderForm.getManualVolumeDivisionTo() >> 56L

        when:
        def request = sut.assembleManualOrderRequest(market, manualOrderForm)

        then:
        request.getOrderPosition() == ORDER_POSITION
        request.getStartRange() == manualOrderForm.getManualVolumeDivisionFrom()
        request.getEndRange() == manualOrderForm.getManualVolumeDivisionTo()
        request.getRequestedVolumeFrom() == manualOrderForm.getManualVolumeRangeFrom()
        request.getRequestedVolumeTo() == manualOrderForm.getManualVolumeRangeTo()
        request.getMarket() == market

        where:
        ORDER_POSITION_STRING | ORDER_POSITION
        "BUY"                 | OrderPosition.BID
        "SELL"                | OrderPosition.ASK
    }
}
