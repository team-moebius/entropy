package com.moebius.entropy.service.order.boboo;

import com.moebius.entropy.domain.inflate.InflationConfig;
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

import java.time.Duration;

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
			.flatMap(tick -> executeMarketOrders(repeatMarketOrderDto))
			.subscribe();

		Disposable bidOrderDisposable = Flux.interval(Duration.ZERO, Duration.ofMillis((long) bidOrderConfig.getPeriod() * 1000L))
			.subscribeOn(Schedulers.parallel())
			.flatMap(tick -> executeMarketOrders(repeatMarketOrderDto))
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

	private Mono<Void> executeMarketOrders(RepeatMarketOrderDto repeatMarketOrderDto) {
		return Mono.when(...)
	}
}
