package com.moebius.entropy.domain.order;

public enum OrderStatus {
	NEW,
	PARTIALLY_FILLED,
	FILLED,
	CANCELED,
	REJECTED,
	PENDING_NEW,
	PENDING
}
