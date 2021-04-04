package com.moebius.entropy.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
			.getOrDefault(disposableId, new ArrayList<>())
			.stream()
			.filter(item -> !item.isDisposed())
			.collect(Collectors.toList());
		disposables.add(disposable);

		disposableOrders.put(disposableId, disposables);
		log.info("[DisposableOrder] Succeeded in setting disposable order info. [{} / {}]", disposableId, disposables);
	}

	public List<String> getKeysBy(Predicate<? super String> condition) {
		return disposableOrders.keySet().stream()
			.filter(condition)
			.collect(Collectors.toList());
	}

	public List<String> getAll() {
		return new ArrayList<>(disposableOrders.keySet());
	}

	public void setAll(String disposableId, List<Disposable> disposables) {
		List<Disposable> fetchedDisposables = disposableOrders.getOrDefault(disposableId, new ArrayList<>());
		fetchedDisposables.addAll(disposables);

		disposableOrders.put(disposableId, fetchedDisposables);
		log.info("[DisposableOrder] Succeeded in setting disposable orders info. [{}]", disposableId);
	}
}
