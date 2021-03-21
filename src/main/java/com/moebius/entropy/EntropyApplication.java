package com.moebius.entropy;

import com.moebius.entropy.service.exchange.ExchangeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class EntropyApplication implements ApplicationListener<ApplicationReadyEvent> {
	private final List<ExchangeService> exchangeServices;

	public static void main(String[] args) {
		SpringApplication.run(EntropyApplication.class, args);
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		log.info("[Entropy] Start to inflate ETHVUSDT orders by current order book.");
		exchangeServices.forEach(exchangeService -> exchangeService.inflateOrdersByOrderBook("ETHVUSDT"));
	}
}
