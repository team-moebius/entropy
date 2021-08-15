package com.moebius.entropy.dto.exchange.orderbook;

import java.util.List;

public interface OrderBookDto<ITEM> {
	String getSymbol();

	List<ITEM> getData();
}
