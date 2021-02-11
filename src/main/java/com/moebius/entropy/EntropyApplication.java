package com.moebius.entropy;

import com.moebius.entropy.service.exchange.ExchangeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
@RequiredArgsConstructor
public class EntropyApplication implements ApplicationListener<ApplicationReadyEvent> {
	private final List<ExchangeService> exchangeServices;

	public static void main(String[] args) {
		SpringApplication.run(EntropyApplication.class, args);
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		exchangeServices.forEach(exchangeService -> exchangeService.getAndLogOrderBook("GTAXUSDT"));
	}
}
