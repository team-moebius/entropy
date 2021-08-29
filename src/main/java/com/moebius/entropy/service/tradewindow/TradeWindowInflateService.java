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
import com.moebius.entropy.util.EntropyRandomUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeWindowInflateService {

	private final TradeWindowQueryService tradeWindowQueryService;
	private final InflationConfigRepository inflationConfigRepository;
	private final OrderServiceFactory orderServiceFactory;
	private final TradeWindowVolumeResolver volumeResolver;
	private final EntropyRandomUtils randomUtils;

	public Flux<Order> inflateOrders(InflateRequest inflateRequest) {
		Market targetMarket = inflateRequest.getMarket();
		InflationConfig inflationConfig = inflationConfigRepository.getConfigFor(targetMarket);
		Market market = Optional.ofNullable(inflationConfig.getMarket())
				.orElse(targetMarket);
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
		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
		BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

		BigDecimal bidStartPrice = marketPrice.subtract(priceUnit);
    Map<String, BigDecimal> bidVolumeBySpreadWindow = mergeTradeWindowBySpreadWindows(
        market, bidStartPrice, inflationConfig.getSpreadWindow(), BigDecimal::subtract,
        window.getBidPrices()
    );
    Flux<OrderRequest> bidRequestFlux = makeOrderRequestWith(
			inflationConfig.getBidCount(), inflationConfig.getBidMinVolume(), market, bidStartPrice,
			OrderPosition.BID, BigDecimal::subtract, inflationConfig.getSpreadWindow(), bidVolumeBySpreadWindow);

		BigDecimal askStartPrice = marketPrice.add(priceUnit);
    Map<String, BigDecimal> askVolumeBySpreadWindow = mergeTradeWindowBySpreadWindows(
        market, askStartPrice, inflationConfig.getSpreadWindow(), BigDecimal::add,
        window.getAskPrices()
    );
		Flux<OrderRequest> askRequestFlux = makeOrderRequestWith(
			inflationConfig.getAskCount(), inflationConfig.getAskMinVolume(), market, askStartPrice,
			OrderPosition.ASK, BigDecimal::add, inflationConfig.getSpreadWindow(), askVolumeBySpreadWindow);

		OrderService orderService = orderServiceFactory.getOrderService(market.getExchange());

		return Flux.merge(bidRequestFlux, askRequestFlux)
			.doOnNext(orderRequest -> log.info("[TradeWindowInflation] Create order for inflation. [{}]", orderRequest))
			.flatMap(orderService::requestOrder)
			.onErrorContinue((throwable, o) -> log.warn("[TradeWindowInflation] Failed to request order.", throwable));
	}

  private Map<String, BigDecimal> mergeTradeWindowBySpreadWindows(
      Market market, BigDecimal startPrice, int spreadWindow, BinaryOperator<BigDecimal> operationOnPrice,
      List<TradePrice> prices
  ) {
    BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();
    BigDecimal stepPriceRange = priceUnit.multiply(BigDecimal.valueOf(spreadWindow));

    Map<String, BigDecimal> volumesBySpreadWindows = new HashMap<>();

    prices.forEach(tradePrice -> {
      BigDecimal price = tradePrice.getUnitPrice();

      //when marketPrice is 11.35 and spreadWindow is 5
      /*
       * ASK
       * if price is 11.36, (11.36-11.35-0.01) / 0.01*5 = 0/0.05 = 0 => spreadStartPrice = 11.36
       * if price is 11.47, (11.47-11.35-0.01) / 0.01*5 = 0.11/0.05 = 2 => spreadStartPrice = (11.35+0.01) + 0.05 * 2 = 11.36+0.10 = 11.46
       * BID
       * if price is 11.34, (11.35-11.34) / 0.01*5 = 0.01/0.05 = 0 => spreadStartPrice = 11.35
       * if price is 11.15, (11.35-11.15) / 0.01*5 = 0.20/0.05 = 4 => spreadStartPrice = 11.35 - 0.05 * 4 = 11.35-0.20 = 11.15
       */

			BigDecimal spreadUnitStep = startPrice.subtract(price).abs()
					.divide(stepPriceRange, 0, RoundingMode.FLOOR);

      BigDecimal spreadStartPrice = operationOnPrice.apply(
					startPrice,
          stepPriceRange.multiply(spreadUnitStep)
      );

      String startPriceString = spreadStartPrice.toPlainString();
      BigDecimal volumeBySpreadWindow = volumesBySpreadWindows.getOrDefault(startPriceString, BigDecimal.ZERO)
          .add(tradePrice.getVolume());
      volumesBySpreadWindows.put(startPriceString, volumeBySpreadWindow);
    });
    return volumesBySpreadWindows;
  }

	private Flux<OrderRequest> makeOrderRequestWith(
		int count, BigDecimal minimumVolume, Market market, BigDecimal startPrice,
		OrderPosition orderPosition, BinaryOperator<BigDecimal> operationOnPrice, int spreadWindow,
		Map<String, BigDecimal> volumesBySpreadWindow
	) {
		BigDecimal priceUnit = market
				.getTradeCurrency()
				.getPriceUnit();

		BigDecimal stepPriceRange = priceUnit
				.multiply(BigDecimal.valueOf(spreadWindow));

		int scale = priceUnit.scale();

		return Flux.range(0, count)
			.map(BigDecimal::valueOf)
			.map(multiplier -> Pair.of(multiplier, operationOnPrice
					.apply(startPrice, stepPriceRange.multiply(multiplier))))
			.filter(pricePair -> volumesBySpreadWindow.getOrDefault(pricePair.getValue().toPlainString(), BigDecimal.ZERO).compareTo(minimumVolume) < 0)
			.map(pricePair->{
				if (spreadWindow == 1){
					return pricePair.getValue();
				}
				BigDecimal startMultiplier = pricePair.getKey();
				BigDecimal endMultiplier = startMultiplier.add(BigDecimal.ONE);
				BigDecimal rangeStartPrice = pricePair.getValue();
				BigDecimal rangeEndPrice = operationOnPrice.apply(startPrice, stepPriceRange.multiply(endMultiplier));
				return randomUtils.getRandomDecimal(rangeStartPrice, rangeEndPrice, scale);
			})
			.map(price -> {
				BigDecimal inflationVolume = volumeResolver.getInflationVolume(market, orderPosition);
				return new OrderRequest(market, orderPosition, price, inflationVolume);
			});
	}

	private Flux<Order> cancelInvalidOrders(Market market, InflationConfig inflationConfig) {
		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
		BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

		int spreadWindow = inflationConfig.getSpreadWindow();
		BigDecimal priceRangeForSteps = priceUnit.multiply(BigDecimal.valueOf(spreadWindow));

		BigDecimal askStartPrice = marketPrice.add(priceUnit);
		BigDecimal maxAskPrice = askStartPrice.add(
			priceRangeForSteps.multiply(BigDecimal.valueOf(inflationConfig.getAskCount()))
		);

		BigDecimal bidStartPrice = marketPrice.subtract(priceUnit);
		BigDecimal minBidPrice = bidStartPrice.subtract(
			priceRangeForSteps.multiply(BigDecimal.valueOf(inflationConfig.getBidCount()))
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
