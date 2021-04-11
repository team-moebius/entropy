package com.moebius.entropy.service.order

import com.moebius.entropy.assembler.BobooOrderExchangeAssembler
import com.moebius.entropy.domain.order.Order
import com.moebius.entropy.domain.order.ApiKey
import com.moebius.entropy.dto.exchange.order.boboo.BobooOpenOrdersDto
import com.moebius.entropy.service.exchange.boboo.BobooExchangeService
import com.moebius.entropy.service.order.boboo.BobooOpenOrderRefreshService
import com.moebius.entropy.service.order.boboo.BobooOrderService
import reactor.core.publisher.Flux
import spock.lang.Specification
import spock.lang.Subject

class BobooOpenOrderRefreshServiceTestSpec extends Specification {
	def mockExchangeService = Mock(BobooExchangeService)
	def mockOrderService = Mock(BobooOrderService)
	def mockAssembler = Mock(BobooOrderExchangeAssembler)
	def accessKey = "some_test_api_key"
	def secretKey = "some_test_secret_key"
	@Subject
	def sut = new BobooOpenOrderRefreshService(mockExchangeService, mockOrderService, mockAssembler, accessKey, secretKey)

  def "Update tracked orders from exchange"(){
        when:
        sut.refreshOpenOrderFromExchange()
        then:
        1 * mockExchangeService.getOpenOrders(_, _ as ApiKey) >> Flux.just(Mock(BobooOpenOrdersDto))
        1 * mockAssembler.convertExchangeOrder(_ as BobooOpenOrdersDto) >> Mock(Order)
  }
}
