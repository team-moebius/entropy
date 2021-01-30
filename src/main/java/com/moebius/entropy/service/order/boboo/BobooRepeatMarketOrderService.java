package com.moebius.entropy.service.order.boboo;

import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.order.RepeatMarketOrderConfig;
import com.moebius.entropy.dto.MarketDto;
import com.moebius.entropy.dto.order.RepeatMarketOrderDto;
import com.moebius.entropy.dto.order.RepeatMarketOrderResponseDto;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.tradewindow.TradeWindowInflationVolumeResolver;
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService;
import com.moebius.entropy.util.EntropyRandomUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BobooRepeatMarketOrderService {
	private static final String DISPOSABLE_ID_POSTFIX = "REPEAT-MARKET-ORDER";

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

		String askOrderDisposableId = market.getExchange() + "-" + market.getSymbol() + "-" + "ASK" + DISPOSABLE_ID_POSTFIX;
		String bidOrderDisposableId = market.getExchange() + "-" + market.getSymbol() + "-" + "BID" + DISPOSABLE_ID_POSTFIX;

		disposableOrderRepository.set(askOrderDisposableId, askOrderDisposable);
		disposableOrderRepository.set(bidOrderDisposableId, bidOrderDisposable);

		return Mono.just(ResponseEntity.ok(RepeatMarketOrderResponseDto.builder()
			.askOrderDisposableId(askOrderDisposableId)
			.bidOrderDisposableId(bidOrderDisposableId)
			.build()));
	}

	private Mono<Void> executeMarketOrders(RepeatMarketOrderDto repeatMarketOrderDto, OrderPosition orderPosition) {
		MarketDto market = repeatMarketOrderDto.getMarket();

		BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market.toDomainEntity());
		BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

		return Mono.fromRunnable(() -> {
			BigDecimal volume = volumeResolver.getInflationVolume(market.toDomainEntity(), orderPosition);

		});
	}

	private Mono<ResponseEntity<?>> stopMarketOrders(String disposableId) {
		Optional.ofNullable(disposableOrderRepository.get(disposableId))
			.ifPresent(disposables -> disposables.forEach(Disposable::dispose));

		log.info("[RepeatMarketOrder] Succeeded to stop repeated market orders. [{}]", disposableId);
		return Mono.just(ResponseEntity.ok(disposableId));
	}
}
