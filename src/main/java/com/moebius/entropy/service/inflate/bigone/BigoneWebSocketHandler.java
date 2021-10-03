package com.moebius.entropy.service.inflate.bigone;

import com.moebius.entropy.assembler.bigone.BigoneAssembler;
import com.moebius.entropy.dto.exchange.orderbook.bigone.BigoneOrderBookDto;
import com.moebius.entropy.service.tradewindow.TradeWindowChangeEventListener;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.thymeleaf.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Builder
final class BigoneWebSocketHandler implements WebSocketHandler {

	private final BigoneInflateService bigoneInflateService;
	private final BigoneAssembler bigoneAssembler;
	private final TradeWindowChangeEventListener tradeWindowChangeEventListener;
	private final String symbol;
	private final String subProtocol;
	private final long timeout;

	@Override
	public List<String> getSubProtocols() {
		return Collections.singletonList(subProtocol);
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		if (StringUtils.isEmpty(symbol)) {
			log.warn("[Bigone] The symbol is empty, please set proper symbol first.");
			return Mono.empty();
		}

		return session
			.send(Mono.just(session.textMessage(bigoneAssembler.assembleOrderBookPayload(symbol))))
			.thenMany(session.receive())
			.subscribeOn(Schedulers.boundedElastic())
			.timeout(Duration.ofMillis(timeout), Schedulers.boundedElastic())
			.map(bigoneAssembler::assembleOrderBookDto)
			.filter(this::isValidOrderBookDto)
			.doOnNext(tradeWindowChangeEventListener::inflateOrdersOnTradeWindowChange)
			.then()
			.doOnError(exception -> log
				.error("[Bigone] Failed to inflate orders of {} by order book.", symbol, exception))
			.doOnTerminate(() -> {
				log.error("[Bigone] Terminated order inflation of {}, retry inflation ...", symbol);
				bigoneInflateService.inflateOrdersByOrderBook(symbol);
			});
	}

	private boolean isValidOrderBookDto(BigoneOrderBookDto dto) {
		return Optional.ofNullable(dto)
			.map(BigoneOrderBookDto::getData)
			.filter(CollectionUtils::isNotEmpty)
			.map(depths -> depths.get(0))
			.isPresent();
	}
}
