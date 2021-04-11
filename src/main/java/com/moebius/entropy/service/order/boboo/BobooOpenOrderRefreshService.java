package com.moebius.entropy.service.order.boboo;

import com.moebius.entropy.assembler.BobooOrderExchangeAssembler;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.service.exchange.boboo.BobooExchangeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class BobooOpenOrderRefreshService {
	private final BobooExchangeService bobooExchangeService;
	private final BobooOrderService bobooOrderService;
	private final BobooOrderExchangeAssembler assembler;
	private final ApiKey apiKeyDto;
	private final static List<String> trackingSymbols = Collections.singletonList("ETHVUSDT");

	public BobooOpenOrderRefreshService(BobooExchangeService bobooExchangeService,
		BobooOrderService bobooOrderService,
		BobooOrderExchangeAssembler assembler,
		@Value("${exchange.boboo.apikey.accessKey}") String accessKey,
		@Value("${exchange.boboo.apikey.secretKey}") String secretKey) {
		this.bobooExchangeService = bobooExchangeService;
		this.bobooOrderService = bobooOrderService;
		this.assembler = assembler;
		apiKeyDto = ApiKey.builder()
			.accessKey(accessKey).secretKey(secretKey)
			.build();
	}

  public void refreshOpenOrderFromExchange(){
        log.info("[BobooOpenOrderRefresh] Start refresh from Boboo");
        Flux.fromIterable(trackingSymbols)
                .flatMap(symbol -> bobooExchangeService.getOpenOrders(symbol, apiKeyDto)
                            .map(assembler::convertExchangeOrder)
                            .collectList()
                            .doOnSuccess(updatedCount -> log.info(
                                    "[BobooOpenOrderRefresh] Updated open orders. count: {}, symbol: {}",
                                    updatedCount, symbol
                            ))
                            .doOnError(throwable -> log.error(
                                    "[BobooOpenOrderRefresh] Failed to update open orders., symbol: {}", symbol
                            ))
                            .switchIfEmpty(Mono.empty())
                            .then()
			).then()
			.subscribe();

		log.info("[BobooOpenOrderRefresh] Finished refresh from Boboo");
	}
}
