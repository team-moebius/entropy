package com.moebius.entropy.repository;

import org.springframework.stereotype.Repository;
import reactor.core.Disposable;

import java.util.HashMap;
import java.util.Map;

@Repository
public class DisposableOrderRepository {
	private final Map<String, Disposable> disposableOrders = new HashMap<>();

	public Disposable get(String orderId) {
		return disposableOrders.getOrDefault(orderId, null);
	}

	public void set(String clientOrderId, Disposable disposable) {
		disposableOrders.put(clientOrderId, disposable);
	}
}
