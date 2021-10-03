package com.moebius.entropy.service.inflate.bigone;

import com.moebius.entropy.assembler.bigone.BigoneAssembler;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.inflate.InflateService;
import com.moebius.entropy.service.tradewindow.TradeWindowChangeEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;

import javax.annotation.PostConstruct;
import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class BigoneInflateService implements InflateService {
	private final static String ORDER_INFLATION_DISPOSABLE_ID_FORMAT = "BIGONE-%s-ORDER-INFLATION";

	private final WebSocketClient webSocketClient;
	private final DisposableOrderRepository disposableOrderRepository;
	private final BigoneAssembler bigoneAssembler;
	private final TradeWindowChangeEventListener tradeWindowChangeEventListener;

	@Value("${exchange.bigone.websocket.sub-protocol}")
	private String subProtocol;
	@Value("${exchange.bigone.websocket.timeout}")
	private long timeout;
	@Value("${exchange.bigone.websocket.uri}")
	private String webSocketUri;

	@Override
	public void inflateOrdersByOrderBook(String symbol) {
		log.info("[Bigone] Start to inflate orders of {} by order book.", symbol);
		disposableOrderRepository.get(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol))
			.forEach(Disposable::dispose);

		BigoneWebSocketHandler bigoneWebSocketHandler = BigoneWebSocketHandler.builder()
			.bigoneInflateService(this)
			.bigoneAssembler(bigoneAssembler)
			.tradeWindowChangeEventListener(tradeWindowChangeEventListener)
			.symbol(symbol)
			.subProtocol(subProtocol)
			.timeout(timeout)
			.build();

		Disposable disposable = webSocketClient.execute(URI.create(webSocketUri), bigoneWebSocketHandler)
			.subscribe();

		disposableOrderRepository.set(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol), disposable);
	}

	@Override
	public Exchange getExchange() {
		return Exchange.BIGONE;
	}
}
