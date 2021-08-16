package com.moebius.entropy.service.inflate.bigone;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.repository.DisposableOrderRepository;
import com.moebius.entropy.service.inflate.InflateService;
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
	private final BigoneWebSocketHandler bigoneWebSocketHandler;

	@Value("${exchange.bigone.websocket.uri}")
	private String webSocketUri;

	@PostConstruct
	public void onCreate() {
		bigoneWebSocketHandler.setBigoneInflateService(this);
	}

	@Override
	public void inflateOrdersByOrderBook(String symbol) {
		log.info("[Bigone] Start to inflate orders of {} by order book.", symbol);
		disposableOrderRepository.get(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol))
			.forEach(Disposable::dispose);

		bigoneWebSocketHandler.setSymbol(symbol);
		Disposable disposable = webSocketClient.execute(URI.create(webSocketUri), bigoneWebSocketHandler)
			.subscribe();

		disposableOrderRepository.set(String.format(ORDER_INFLATION_DISPOSABLE_ID_FORMAT, symbol), disposable);
	}

	@Override
	public Exchange getExchange() {
		return Exchange.BIGONE;
	}
}
