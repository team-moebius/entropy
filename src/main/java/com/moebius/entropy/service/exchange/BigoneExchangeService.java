package com.moebius.entropy.service.exchange;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.service.inflate.InflateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BigoneExchangeService implements InflateService {

	@Override
	public void inflateOrdersByOrderBook(String symbol) {

	}

	@Override
	public Exchange getExchange() {
		return Exchange.BIGONE;
	}
}
