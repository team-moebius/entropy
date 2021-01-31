package com.moebius.entropy.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.Disposable;

@Slf4j
@Repository
public class DisposableOrderRepository {
	private final Map<String, List<Disposable>> disposableOrders = new ConcurrentHashMap<>();

	public List<Disposable> get(String disposableId) {
		return disposableOrders.getOrDefault(disposableId, Collections.emptyList());
	}

	public void set(String disposableId, Disposable disposable) {
		List<Disposable> disposables = disposableOrders
			.getOrDefault(disposableId, new ArrayList<>());
		disposables.add(disposable);

		disposableOrders.put(disposableId, disposables);
		log.info("[DisposableOrder] Succeeded in setting disposable order info. [{}]",
			disposableId);
	}

	public List<String> getAll() {
		return new ArrayList<>(disposableOrders.keySet());
	}
}
