package com.moebius.entropy.service.order.boboo.auto;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.order.RepeatMarketOrderConfig;
import com.moebius.entropy.dto.MarketDto;
import com.moebius.entropy.dto.order.RepeatMarketOrderDto;
import com.moebius.entropy.dto.order.RepeatMarketOrderResponseDto;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.order.boboo.BobooOrderService;
import com.moebius.entropy.service.tradewindow.TradeWindowInflationVolumeResolver;
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService;
import com.moebius.entropy.util.EntropyRandomUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class BobooRepeatMarketOrderService {
	private final static String DISPOSABLE_ID_POSTFIX = "REPEAT-MARKET-ORDER";
	private final static int DECIMAL_POSITION = 1;

	private final BobooOrderService orderService;
	private final TradeWindowQueryService tradeWindowQueryService;
	private final TradeWindowInflationVolumeResolver volumeResolver;
	private final DisposableOrderRepository disposableOrderRepository;
	private final EntropyRandomUtils randomUtils;

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
			.flatMap(tick -> executeMarketOrders(repeatMarketOrderDto, OrderPosition.ASK))
			.subscribe();

		Disposable bidOrderDisposable = Flux.interval(Duration.ZERO, Duration.ofMillis((long) bidOrderConfig.getPeriod() * 1000L))
			.subscribeOn(Schedulers.parallel())
			.flatMap(tick -> executeMarketOrders(repeatMarketOrderDto, OrderPosition.BID))
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

	private Mono<Void> executeMarketOrders(RepeatMarketOrderDto repeatMarketOrderDto, OrderPosition orderPosition) {
		return Mono.fromRunnable(() -> {
			MarketDto marketDto = repeatMarketOrderDto.getMarket();
			Market market = marketDto.toDomainEntity();

			BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
			BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

			OrderRequest orderRequest = null;
			int reorderCount = 0;
			Duration orderDuration = Duration.ZERO;

			if (orderPosition == OrderPosition.ASK) {
				RepeatMarketOrderConfig askOrderConfig = repeatMarketOrderDto.getAskOrderConfig();
				BigDecimal volume = volumeResolver.getRandomMarketVolume(askOrderConfig.getMinVolume(), askOrderConfig.getMaxVolume(),
					DECIMAL_POSITION);

				orderRequest = new OrderRequest(market, orderPosition, marketPrice.subtract(priceUnit), volume);
				reorderCount = randomUtils.getRandomInteger(askOrderConfig.getMinReorderCount(), askOrderConfig.getMaxReorderCount());
				orderDuration = Duration.ofMillis((long) (askOrderConfig.getPeriod() / reorderCount * 1000));
			} else if (orderPosition == OrderPosition.BID) {
				RepeatMarketOrderConfig bidOrderConfig = repeatMarketOrderDto.getBidOrderConfig();
				BigDecimal volume = volumeResolver.getRandomMarketVolume(bidOrderConfig.getMinVolume(), bidOrderConfig.getMaxVolume(),
					DECIMAL_POSITION);

				orderRequest = new OrderRequest(market, orderPosition, marketPrice, volume);
				reorderCount = randomUtils.getRandomInteger(bidOrderConfig.getMinReorderCount(), bidOrderConfig.getMaxReorderCount());
				orderDuration = Duration.ofMillis((long) (bidOrderConfig.getPeriod() / reorderCount * 1000));
			}

			final Duration finalOrderDuration = orderDuration;
			final OrderRequest finalOrderRequest = orderRequest;

			Mono.just(finalOrderRequest)
				.repeat(reorderCount - 1)
				.flatMap(orderService::requestOrderWithoutTracking)
				.delayElements(finalOrderDuration)
				.doOnError(throwable -> log.error("[RepeatMarketOrder] Failed to request market order. {}]",
					((WebClientResponseException) throwable).getResponseBodyAsString()))
				.subscribe();
		});
	}
}
