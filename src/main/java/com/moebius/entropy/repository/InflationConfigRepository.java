package com.moebius.entropy.repository;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class InflationConfigRepository {
	private final Map<String, InflationConfig> configMap = new HashMap<>();

	public InflationConfig getConfigFor(Market market) {
		String key = market.getSymbol();

		return configMap.getOrDefault(key, InflationConfig.builder().build());
	}

	public void saveConfigFor(Market market, InflationConfig inflationConfig) {
		if (market == null) {
			throw new RuntimeException("Market is null while saving inflation configuration");
		}
		Optional.ofNullable(inflationConfig)
			.ifPresentOrElse(config -> {
				String key = market.getSymbol();
				configMap.put(key, inflationConfig);
			}, () -> {
				throw new RuntimeException("Inflation configuration is null and tried to save it.");
			});

	}
}
