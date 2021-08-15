package com.moebius.entropy;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.Symbol;
import com.moebius.entropy.service.exchange.ExchangeService;
import com.moebius.entropy.service.inflate.InflateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class EntropyApplication implements ApplicationListener<ApplicationReadyEvent> {
	private final List<InflateService> inflateServices;
	private final Map<Exchange, List<Symbol>> symbols;

	public static void main(String[] args) {
		SpringApplication.run(EntropyApplication.class, args);
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		inflateServices.forEach(inflateService ->
			symbols.getOrDefault(inflateService.getExchange(), Collections.emptyList()).stream()
				.map(Enum::toString)
				.forEach(inflateService::inflateOrdersByOrderBook));
	}
}
