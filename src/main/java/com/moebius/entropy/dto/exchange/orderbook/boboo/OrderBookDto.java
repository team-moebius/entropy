package com.moebius.entropy.dto.exchange.orderbook.boboo;

import java.util.List;

public interface OrderBookDto<DATA> {
	String getSymbol();

	List<DATA> getData();
}
