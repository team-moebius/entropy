package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflateRequest;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.InflationConfigRepository;
import com.moebius.entropy.service.order.OrderService;
import com.moebius.entropy.service.order.OrderServiceFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeWindowInflateService {

	private final static int START_FROM_MARKET_PRICE = 0;
	private final static int START_FROM_NEXT_PRICE = 1;

	private final TradeWindowQueryService tradeWindowQueryService;
	private final InflationConfigRepository inflationConfigRepository;
	private final OrderServiceFactory orderServiceFactory;
	private final TradeWindowVolumeResolver volumeResolver;

	public Flux<Order> inflateOrders(InflateRequest inflateRequest) {
		Market market = inflateRequest.getMarket();
		InflationConfig inflationConfig = inflationConfigRepository.getConfigFor(market);
		if (!inflationConfig.isEnable()) {
			return Flux.empty();
		}

		return tradeWindowQueryService.getTradeWindowMono(market)
			.flatMapMany(tradeWindow -> {
				Flux<Order> requestOrderFlux = requestRequiredOrders(market, tradeWindow, inflationConfig);
				Flux<Order> cancelOrderFlux = cancelInvalidOrders(market, inflationConfig);

				return Flux.merge(requestOrderFlux, cancelOrderFlux);
			})
			.onErrorContinue((throwable, o) -> log.warn("[TradeWindowInflation] Failed to collect order result.", throwable));
	}

	private Flux<Order> requestRequiredOrders(Market market, TradeWindow window, InflationConfig inflationConfig) {
		Flux<OrderRequest> bidRequestFlux = makeOrderRequestWith(
			START_FROM_MARKET_PRICE, inflationConfig.getBidCount(), inflationConfig.getBidMinVolume(), market,
			OrderPosition.BID, BigDecimal::subtract,
			window.getBidPrices());

		Flux<OrderRequest> askRequestFlux = makeOrderRequestWith(
			START_FROM_NEXT_PRICE, inflationConfig.getAskCount(), inflationConfig.getAskMinVolume(), market,
			OrderPosition.ASK, BigDecimal::add,
			window.getAskPrices());

		OrderService orderService = orderServiceFactory.getOrderService(market.getExchange());

		return Flux.merge(bidRequestFlux, askRequestFlux)
			.doOnNext(orderRequest -> log.info("[TradeWindowInflation] Create order for inflation. [{}]", orderRequest))
			.flatMap(orderService::requestOrder)
			.onErrorContinue((throwable, o) -> log.warn("[TradeWindowInflation] Failed to request order.", throwable));
	}

	private Flux<OrderRequest> makeOrderRequestWith(
		int startFrom, int count, BigDecimal minimumVolume, Market market, OrderPosition orderPosition,
		BinaryOperator<BigDecimal> priceCalculationHandler,
		List<TradePrice> prices
	) {
		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market)
			.setScale(market.getPriceDecimalPosition(), RoundingMode.HALF_UP);
		BigDecimal startPrice = OrderPosition.BID.equals(orderPosition)
			? marketPrice.subtract(market.getTradeCurrency().getPriceUnit())
			: marketPrice;

		BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();
		BigDecimal highestBidPrice = marketPrice.subtract(priceUnit);
		Map<String, BigDecimal> priceVolumeMap = prices.stream()
			.collect(Collectors.toMap(tradePrice -> tradePrice.getUnitPrice().toPlainString(),
				TradePrice::getVolume));

		return Flux.range(startFrom, count)
			.map(BigDecimal::valueOf)
			.map(multiplier -> priceCalculationHandler
				.apply(startPrice, priceUnit.multiply(multiplier)))
			.filter(price -> price.compareTo(marketPrice) != 0 &&
				price.compareTo(highestBidPrice) != 0 &&
				(!priceVolumeMap.containsKey(price.toPlainString())
					|| priceVolumeMap.get(price.toPlainString()).compareTo(minimumVolume) < 0))
			.map(price -> {
				BigDecimal inflationVolume = volumeResolver.getInflationVolume(market,
					orderPosition);
				return new OrderRequest(market, orderPosition, price, inflationVolume);
			});
	}

	private Flux<Order> cancelInvalidOrders(Market market, InflationConfig inflationConfig) {
		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
		BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

		BigDecimal maxAskPrice = marketPrice.add(
			priceUnit.multiply(BigDecimal.valueOf(inflationConfig.getAskCount() + START_FROM_NEXT_PRICE))
		);

		BigDecimal minBidPrice = marketPrice.subtract(
			priceUnit.multiply(BigDecimal.valueOf(inflationConfig.getBidCount()))
		);

		OrderService orderService = orderServiceFactory.getOrderService(market.getExchange());

		return orderService.fetchAllOrdersFor(market)
			.filter(order -> {
				BigDecimal orderPrice = order.getPrice();
				if (OrderPosition.ASK.equals(order.getOrderPosition())) {
					return orderPrice.compareTo(maxAskPrice) > 0;
				} else {
					return orderPrice.compareTo(minBidPrice) < 0;
				}
			})
			.doOnNext(order -> log.info("[TradeWindowInflation] Cancel order for inflation. [{}]", order))
			.flatMap(orderService::cancelOrder)
			.onErrorContinue((throwable, o) -> log.warn("[TradeWindowInflation] Failed to cancel order.", throwable));
	}
}
