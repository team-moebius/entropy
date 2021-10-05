package com.moebius.entropy.service.inflate.bigone;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.exchange.bigone.BigoneExchangeService;
import com.moebius.entropy.service.inflate.InflateService;
import com.moebius.entropy.service.tradewindow.TradeWindowChangeEventListener;
import com.moebius.entropy.util.SymbolUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class BigoneInflateService implements InflateService {

	private final static String ORDER_INFLATION_DISPOSABLE_ID_FORMAT = "BIGONE-%s-ORDER-INFLATION";

	private final DisposableOrderRepository disposableOrderRepository;
	private final BigoneExchangeService bigoneExchangeService;
	private final TradeWindowChangeEventListener tradeWindowChangeEventListener;

	@Override
	public void inflateOrdersByOrderBook(String symbol) {
		log.info("[Bigone] Start to inflate orders of {} by order book.", symbol);
		disposableOrderRepository.get(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol))
			.forEach(Disposable::dispose);

		Disposable disposable = Mono.defer(() -> bigoneExchangeService.getOrderBook(
			SymbolUtil.addDashBeforeBaseCurrency(symbol)))
			.repeat()
			.doOnNext(tradeWindowChangeEventListener::inflateOrdersOnTradeWindowChange)
			.doOnError(exception -> log
				.error("[Bigone] Failed to inflate orders of {} by order book.", symbol, exception))
			.doOnTerminate(() -> {
				log.error("[Bigone] Terminated order inflation of {}, retry inflation ...", symbol);
				inflateOrdersByOrderBook(symbol);
			})
			.subscribe();

		disposableOrderRepository
			.set(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol), disposable);
	}

	@Override
	public Exchange getExchange() {
		return Exchange.BIGONE;
	}
}
