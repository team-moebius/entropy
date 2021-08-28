package com.moebius.entropy.configuration;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Symbol;
import com.moebius.entropy.domain.order.ApiKey;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "entropy")
public class EntropyConfiguration {
	private Map<String, List<String>> symbols;
	private Map<String, Map<String, ApiKey>> apiKeys;

	@Bean
	Map<Exchange, List<Symbol>> symbols() {
		return symbols.keySet().stream()
			.collect(Collectors.toMap(exchange -> Exchange.valueOf(exchange.toUpperCase()),
				exchange -> symbols.get(exchange).stream()
					.map(Symbol::valueOf)
					.collect(Collectors.toList())));
	}

	@Bean
	Map<Exchange, Map<Symbol, ApiKey>> apiKeys() {
		return apiKeys.keySet().stream()
			.collect(Collectors.toMap(exchange -> Exchange.valueOf(exchange.toUpperCase()),
				exchange -> apiKeys.get(exchange).keySet().stream()
					.collect(Collectors.toMap(symbol -> Symbol.valueOf(symbol.toUpperCase()),
						symbol -> apiKeys.get(exchange).get(symbol)))));
	}
}
