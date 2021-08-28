package com.moebius.entropy.service.inflate;

import com.moebius.entropy.domain.Exchange;

public interface InflateService {
	void inflateOrdersByOrderBook(String symbol);

	Exchange getExchange();
}
