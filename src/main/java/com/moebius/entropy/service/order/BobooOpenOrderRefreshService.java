package com.moebius.entropy.service.order;

import com.moebius.entropy.assembler.OrderBobooExchangeAssembler;
import com.moebius.entropy.dto.exchange.order.ApiKeyDto;
import com.moebius.entropy.service.exchange.BobooService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class BobooOpenOrderRefreshService {
    private final BobooService bobooService;
    private final BobooOrderService bobooOrderService;
    private final OrderBobooExchangeAssembler assembler;
    private final ApiKeyDto apiKeyDto;
    private static final List<String> trackingSymbols = Collections.singletonList("GTAXUSDT");

    public BobooOpenOrderRefreshService(BobooService bobooService,
                                        BobooOrderService bobooOrderService,
                                        OrderBobooExchangeAssembler assembler,
                                        @Value("${exchange.boboo.apikey.access-key}") String accessKey,
                                        @Value("${exchange.boboo.apikey.secret-key}") String secretKey) {
        this.bobooService = bobooService;
        this.bobooOrderService = bobooOrderService;
        this.assembler = assembler;
        apiKeyDto = ApiKeyDto.builder()
                .accessKey(accessKey).secretKey(secretKey)
                .build();
    }

    @Scheduled(cron = "0 * * * * *")
    public void refreshOpenOrderFromExchange(){
        log.info("[BobooOpenOrderRefresh] Start refresh from Boboo");
        Flux.fromIterable(trackingSymbols)
                .flatMap(symbol-> bobooService.getOpenOrders(symbol, apiKeyDto)
                            .map(assembler::convertExchangeOrder)
                            .collectList()
                            .map(bobooOrderService::updateOrders)
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
