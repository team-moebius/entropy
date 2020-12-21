package com.moebius.entropy.dto.exchange.orderbook.response;

import java.util.List;

public interface OrderBookDto<DATA> {
	String getSymbol();

	List<DATA> getData();
}
