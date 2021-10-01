package com.moebius.entropy.service.view

import com.moebius.entropy.assembler.AutomaticOrderViewAssembler
import com.moebius.entropy.assembler.ManualOrderRequestAssembler
import com.moebius.entropy.domain.Exchange
import com.moebius.entropy.domain.ManualOrderMakingRequest
import com.moebius.entropy.domain.ManualOrderResult
import com.moebius.entropy.domain.Market
import com.moebius.entropy.domain.inflate.InflationConfig
import com.moebius.entropy.domain.order.Order
import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.dto.order.DividedDummyOrderDto
import com.moebius.entropy.dto.order.RepeatMarketOrderDto
import com.moebius.entropy.dto.view.AutomaticOrderCancelForm
import com.moebius.entropy.dto.view.AutomaticOrderForm
import com.moebius.entropy.dto.view.AutomaticOrderResult
import com.moebius.entropy.dto.view.ManualOrderForm
import com.moebius.entropy.repository.DisposableOrderRepository
import com.moebius.entropy.repository.InflationConfigRepository
import com.moebius.entropy.service.order.OrderService
import com.moebius.entropy.service.order.OrderServiceFactory
import com.moebius.entropy.service.order.auto.DividedDummyOrderService
import com.moebius.entropy.service.order.auto.OptimizeOrderService
import com.moebius.entropy.service.order.auto.RepeatMarketOrderService
import com.moebius.entropy.service.order.manual.ManualOrderMakerService
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService
import org.apache.commons.collections4.CollectionUtils
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification

import java.time.Duration
import java.util.function.Predicate

class EntropyViewServiceTestSpec extends Specification {
	def market = new Market(Exchange.BOBOO, "GTAX2USDT", TradeCurrency.USDT, 2, 2)
	def disposableIds = ["test-disposable-id", "test-disposable-id2"]
	def automaticOrderViewAssembler = Mock(AutomaticOrderViewAssembler)
	def manualOrderViewAssembler = Mock(ManualOrderRequestAssembler)
	def manualOrderMakerService = Mock(ManualOrderMakerService)
	def dividedDummyOrderService = Mock(DividedDummyOrderService)
	def repeatMarketOrderService = Mock(RepeatMarketOrderService)
	def optimizeOrderService = Mock(OptimizeOrderService)
	def inflationConfigRepository = Mock(InflationConfigRepository)
	def orderServiceFactory = Mock(OrderServiceFactory)
	def disposableOrderRepository = Mock(DisposableOrderRepository)
	def tradeWindowQueryService = Mock(TradeWindowQueryService)

	//TBD for rest service
	def sut = new EntropyViewService(
			automaticOrderViewAssembler, dividedDummyOrderService, repeatMarketOrderService, optimizeOrderService, inflationConfigRepository,
			orderServiceFactory, manualOrderViewAssembler, manualOrderMakerService, disposableOrderRepository, tradeWindowQueryService
	)


	def "Should request to start automatic orders from request form"() {
		given:
		def orderForm = Stub(AutomaticOrderForm)
		def inflationConfig = Stub(InflationConfig)
		def dividedDummyOrderDto = Stub(DividedDummyOrderDto)
		def repeatMarketOrderDto = Stub(RepeatMarketOrderDto)

		1 * inflationConfigRepository.saveConfigFor(market, inflationConfig)
		1 * automaticOrderViewAssembler.assembleInitialInflationConfig(orderForm) >> inflationConfig
		1 * automaticOrderViewAssembler.assembleDivideDummyOrder(market, orderForm) >> dividedDummyOrderDto
		1 * automaticOrderViewAssembler.assembleRepeatMarketOrder(market, orderForm) >> repeatMarketOrderDto
		1 * dividedDummyOrderService.executeDividedDummyOrders(dividedDummyOrderDto) >> Mono.just(
				ResponseEntity.ok("test-disposable-id")
		)
		1 * repeatMarketOrderService.executeRepeatMarketOrders(repeatMarketOrderDto) >> Mono.just(
				ResponseEntity.ok("test-disposable-id2")
		)
		1 * automaticOrderViewAssembler.assembleAutomaticOrderResult(disposableIds) >> AutomaticOrderResult.builder()
				.disposableIds(disposableIds)
				.build()
		1 * optimizeOrderService.optimizeOrders(market) >> Flux.just(Stub(Order))

		expect:
		StepVerifier.create(sut.startAutomaticOrder(market, orderForm))
                .expectNextMatches(result -> {
                    return result != null &&
                            CollectionUtils.isNotEmpty(result.getDisposableIds()) &&
                            result.getDisposableIds().contains("test-disposable-id") &&
                            result.getDisposableIds().contains("test-disposable-id2")
                })
                .verifyComplete()
	}

	def "Should cancel ongoing automatic orders"() {
		given:
		def cancelForm = Mock(AutomaticOrderCancelForm)
		cancelForm.getMarket() >> market
		disposableOrderRepository.getAll() >> disposableIds
		1 * inflationConfigRepository.getConfigFor(market) >> InflationConfig.builder()
				.enable(true)
				.build()
		1 * inflationConfigRepository.saveConfigFor(market, { it.enable == false })
		1 * orderServiceFactory.getOrderService(_ as Exchange) >> Stub(OrderService) {
			stopOrder("test-disposable-id") >> Mono.just(ResponseEntity.ok("test-disposable-id"))
			stopOrder("test-disposable-id2") >> Mono.just(ResponseEntity.ok("test-disposable-id2"))
		}
		1 * disposableOrderRepository.getKeysBy(_ as Predicate) >> ["test-disposable-id", "test-disposable-id2"]

		expect:
		StepVerifier.create(sut.cancelAutomaticOrder(cancelForm))
				.expectNextMatches(result -> {
					return result != null &&
                            CollectionUtils.isNotEmpty(result.getCancelledDisposableIds()) &&
                            result.getCancelledDisposableIds().contains("test-disposable-id") &&
                            result.getCancelledDisposableIds().contains("test-disposable-id2") &&
                            result.inflationCancelled
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
				.assertNext({ assert it instanceof ManualOrderResult })
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
					assert it.getSymbol() == "GTAX2"
					assert it.getPrice() == prices[0]
				})
				.thenAwait(Duration.ofSeconds(intervalSeconds))
				.assertNext({
					assert it.getExchange() == market.getExchange()
					assert it.getTradeCurrency() == market.getTradeCurrency().name()
					assert it.getPriceUnit() == market.getTradeCurrency().priceUnit
					assert it.getSymbol() == "GTAX2"
					assert it.getPrice() == prices[1]
				})
				.thenAwait(Duration.ofSeconds(intervalSeconds))
				.assertNext({
					assert it.getExchange() == market.getExchange()
					assert it.getTradeCurrency() == market.getTradeCurrency().name()
					assert it.getPriceUnit() == market.getTradeCurrency().priceUnit
					assert it.getSymbol() == "GTAX2"
					assert it.getPrice() == prices[2]
				})
				.verifyComplete()

	}
}
