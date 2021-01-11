package com.moebius.entropy.repository;

import org.springframework.stereotype.Repository;
import reactor.core.Disposable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DisposableOrderRepository {
	private final Map<String, List<Disposable>> disposableOrders = new ConcurrentHashMap<>();

	public List<Disposable> get(String disposableId) {
		return disposableOrders.getOrDefault(disposableId, Collections.emptyList());
	}

	public void set(String disposableId, Disposable disposable) {
		List<Disposable> disposables = disposableOrders.getOrDefault(disposableId, new ArrayList<>());
		disposables.add(disposable);

		disposableOrders.put(disposableId, disposables);
	}
}
