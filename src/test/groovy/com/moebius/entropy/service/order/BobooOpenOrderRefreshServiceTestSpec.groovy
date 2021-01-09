package com.moebius.entropy.service.order

import com.moebius.entropy.assembler.OrderBobooExchangeAssembler
import com.moebius.entropy.domain.Order
import com.moebius.entropy.dto.exchange.order.ApiKeyDto
import com.moebius.entropy.dto.exchange.order.boboo.BobooOpenOrdersDto
import com.moebius.entropy.service.exchange.BobooService
import reactor.core.publisher.Flux
import spock.lang.Specification
import spock.lang.Subject

class BobooOpenOrderRefreshServiceTestSpec extends Specification{
    def mockExchangeService = Mock(BobooService)
    def mockOrderService = Mock(BobooOrderService)
    def mockAssembler = Mock(OrderBobooExchangeAssembler)
    def accessKey = "some_test_api_key"
    def secretKey = "some_test_secret_key"
    @Subject
    def sut = new BobooOpenOrderRefreshService(mockExchangeService, mockOrderService, mockAssembler, accessKey, secretKey)

    def "Update tracked orders from exchange"(){
        when:
        sut.refreshOpenOrderFromExchange()
        then:
        1 * mockExchangeService.getOpenOrders("GTAXUSDT", _ as ApiKeyDto) >> Flux.just(Mock(BobooOpenOrdersDto))
        1 * mockAssembler.convertExchangeOrder(_ as BobooOpenOrdersDto) >> Mock(Order)
        1 * mockOrderService.updateOrders({List<Order> orders->
            orders.size() == 1
        })
    }
}
