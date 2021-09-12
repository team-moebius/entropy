package com.moebius.entropy.service.order.auto;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.order.config.RepeatMarketOrderConfig;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.dto.MarketDto;
import com.moebius.entropy.dto.order.RepeatMarketOrderDto;
import com.moebius.entropy.dto.order.RepeatMarketOrderResponseDto;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.order.OrderService;
import com.moebius.entropy.service.order.OrderServiceFactory;
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService;
import com.moebius.entropy.service.tradewindow.TradeWindowVolumeResolver;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepeatMarketOrderService {
	private final static String DISPOSABLE_ID_POSTFIX = "REPEAT-MARKET-ORDER";

	private final OrderServiceFactory orderServiceFactory;
	private final TradeWindowQueryService tradeWindowQueryService;
	private final TradeWindowVolumeResolver volumeResolver;
	private final DisposableOrderRepository disposableOrderRepository;

	public Mono<ResponseEntity<?>> executeRepeatMarketOrders(RepeatMarketOrderDto repeatMarketOrderDto) {
		if (repeatMarketOrderDto == null) {
			return Mono.just(ResponseEntity.badRequest().build());
		}

		MarketDto market = repeatMarketOrderDto.getMarket();
		RepeatMarketOrderConfig askOrderConfig = repeatMarketOrderDto.getAskOrderConfig();
		RepeatMarketOrderConfig bidOrderConfig = repeatMarketOrderDto.getBidOrderConfig();

		if (market == null || askOrderConfig == null || bidOrderConfig == null) {
			return Mono.just(ResponseEntity.badRequest().build());
		}

		Disposable askOrderDisposable = Flux.interval(Duration.ZERO, Duration.ofMillis((long) askOrderConfig.getPeriod() * 1000L))
			.subscribeOn(Schedulers.parallel())
			.flatMap(tick -> getMarketOrderMono(repeatMarketOrderDto, OrderPosition.ASK))
			.subscribe();

		Disposable bidOrderDisposable = Flux.interval(Duration.ZERO, Duration.ofMillis((long) bidOrderConfig.getPeriod() * 1000L))
			.subscribeOn(Schedulers.parallel())
			.flatMap(tick -> getMarketOrderMono(repeatMarketOrderDto, OrderPosition.BID))
			.subscribe();

		String askOrderDisposableId = market.getExchange() + "-" + market.getSymbol() + "-" + "ASK-" + DISPOSABLE_ID_POSTFIX;
		String bidOrderDisposableId = market.getExchange() + "-" + market.getSymbol() + "-" + "BID-" + DISPOSABLE_ID_POSTFIX;

		disposableOrderRepository.set(askOrderDisposableId, askOrderDisposable);
		disposableOrderRepository.set(bidOrderDisposableId, bidOrderDisposable);

		return Mono.just(ResponseEntity.ok(RepeatMarketOrderResponseDto.builder()
			.askOrderDisposableId(askOrderDisposableId)
			.bidOrderDisposableId(bidOrderDisposableId)
			.build()));
	}

	private Mono<Order> getMarketOrderMono(RepeatMarketOrderDto repeatMarketOrderDto, OrderPosition orderPosition) {
		MarketDto marketDto = repeatMarketOrderDto.getMarket();
		Market market = marketDto.toDomainEntity();

		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
		TradeWindow tradeWindow = tradeWindowQueryService.getTradeWindow(market);
		BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

		OrderRequest orderRequest = null;
		BigDecimal targetPrice;

		if (orderPosition == OrderPosition.ASK) {
			targetPrice = tradeWindow.getBidPrices()
				.stream()
				.max(Comparator.comparing(TradePrice::getUnitPrice))
				.map(TradePrice::getUnitPrice)
				.orElse(marketPrice.subtract(priceUnit));
			RepeatMarketOrderConfig askOrderConfig = repeatMarketOrderDto.getAskOrderConfig();
			BigDecimal volume = volumeResolver.getRandomMarketVolume(askOrderConfig.getMinVolume(),
				askOrderConfig.getMaxVolume(),
				market.getVolumeDecimalPosition());

			orderRequest = new OrderRequest(market, orderPosition, targetPrice, volume);
		} else if (orderPosition == OrderPosition.BID) {
			targetPrice = tradeWindow.getAskPrices()
				.stream()
				.min(Comparator.comparing(TradePrice::getUnitPrice))
				.map(TradePrice::getUnitPrice)
				.orElse(marketPrice);

			RepeatMarketOrderConfig bidOrderConfig = repeatMarketOrderDto.getBidOrderConfig();
			BigDecimal volume = volumeResolver.getRandomMarketVolume(bidOrderConfig.getMinVolume(),
				bidOrderConfig.getMaxVolume(),
				market.getVolumeDecimalPosition());

			orderRequest = new OrderRequest(market, orderPosition, targetPrice, volume);
		}

		if (orderRequest == null) {
			return Mono.empty();
		}

		OrderService orderService = orderServiceFactory.getOrderService(market.getExchange());

		return Mono.just(orderRequest)
			.flatMap(orderService::requestOrder)
			.doOnNext(order -> log.info("[RepeatMarketOrder] Create repeated market order request. [{}]", order))
			.doOnError(throwable -> log.error("[RepeatMarketOrder] Failed to request market order.", throwable));
	}
}
