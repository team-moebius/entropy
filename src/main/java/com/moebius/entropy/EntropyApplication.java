package com.moebius.entropy;

import com.moebius.entropy.service.exchange.ExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class EntropyApplication implements ApplicationListener<ApplicationReadyEvent> {
	private final List<ExchangeService> exchangeServices;
	@Value("#{'${entropy.symbols}'.split(',')}")
	private List<String> symbols;

	public static void main(String[] args) {
		SpringApplication.run(EntropyApplication.class, args);
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		exchangeServices.forEach(exchangeService -> symbols.forEach(exchangeService::inflateOrdersByOrderBook));
	}
}
