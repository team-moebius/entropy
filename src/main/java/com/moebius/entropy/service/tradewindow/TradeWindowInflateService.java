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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeWindowInflateService {

	private final static int START_FROM_MARKET_PRICE = 0;
	private final static int START_FROM_NEXT_PRICE = 1;

	private final TradeWindowQueryService tradeWindowQueryService;
	private final InflationConfigRepository inflationConfigRepository;
	private final OrderService orderService;
	private final TradeWindowInflationVolumeResolver volumeResolver;
	private final BobooTradeWindowChangeEventListener windowChangeEventListener;

	@SuppressWarnings("unused")
	@PostConstruct
	public void onCreate() {
		windowChangeEventListener.setTradeWindowInflateService(this);
	}

	public Flux<Order> inflateOrders(InflateRequest inflateRequest) {
		Market market = inflateRequest.getMarket();
		InflationConfig inflationConfig = inflationConfigRepository.getConfigFor(market);
		if (!inflationConfig.isEnable()) {
			return Flux.empty();
		}

		return tradeWindowQueryService.fetchTradeWindow(market)
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

		return Flux.merge(bidRequestFlux, askRequestFlux)
			.doOnNext(orderRequest -> log.info("[TradeWindowInflation] Create order requests for inflation. [{}]", orderRequest))
			.flatMap(orderService::requestOrder)
			.onErrorContinue((throwable, o) -> log.warn("[TradeWindowInflation] Failed to request order. [{}]", ((WebClientResponseException) throwable).getResponseBodyAsString(), throwable));
	}

	private Flux<OrderRequest> makeOrderRequestWith(
		int startFrom, int count, BigDecimal minimumVolume, Market market, OrderPosition orderPosition,
		BinaryOperator<BigDecimal> priceCalculationHandler,
		List<TradePrice> prices
	) {
		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
		BigDecimal startPrice = OrderPosition.BID.equals(orderPosition)
			? marketPrice.subtract(market.getTradeCurrency().getPriceUnit())
			: marketPrice;

		BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();
		BigDecimal highestBidPrice = marketPrice.subtract(priceUnit);
		Map<Float, Float> priceVolumeMap = prices.stream()
			.collect(Collectors.toMap(tradePrice -> tradePrice.getUnitPrice().floatValue(), tradePrice -> tradePrice.getVolume().floatValue()));

		return Flux.range(startFrom, count)
			.map(BigDecimal::valueOf)
			.map(multiplier -> priceCalculationHandler
				.apply(startPrice, priceUnit.multiply(multiplier)))
			.filter(price -> price.compareTo(marketPrice) != 0 &&
				price.compareTo(highestBidPrice) != 0 &&
				(!priceVolumeMap.containsKey(price.floatValue()) || priceVolumeMap.get(price.floatValue()) < minimumVolume.floatValue()))
			.map(price -> {
				BigDecimal inflationVolume = volumeResolver.getInflationVolume(market, orderPosition);
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

		return orderService.fetchAutomaticOrdersFor(market)
			.filter(order -> {
				BigDecimal orderPrice = order.getPrice();
				if (OrderPosition.ASK.equals(order.getOrderPosition())) {
					return orderPrice.compareTo(maxAskPrice) > 0;
				} else {
					return orderPrice.compareTo(minBidPrice) < 0;
				}
			})
			.doOnNext(order -> log.info("[TradeWindowInflation] Create order cancellations for inflation. [{}]", order))
			.flatMap(orderService::cancelOrder)
			.onErrorContinue((throwable, o) -> log.warn("[TradeWindowInflation] Failed to cancel order. [{}]", ((WebClientResponseException) throwable).getResponseBodyAsString(), throwable));
	}
}
