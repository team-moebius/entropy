package com.moebius.entropy.service.exchange.hotbit;

import com.moebius.entropy.assembler.hotbit.HotbitAssembler;
import com.moebius.entropy.configuration.hotbit.HotbitConfiguration;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.dto.exchange.order.hotbit.*;
import com.moebius.entropy.service.exchange.ExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotbitExchangeService implements ExchangeService<
        HotbitCancelRequestDto, HotbitCancelResponseDto,
        HotbitRequestOrderDto,
        HotbitRequestOrderResponseDto, HotbitOpenOrderResponseDto> {
    private final WebClient webClient;
    private final HotbitConfiguration hotbitConfiguration;
    private final HotbitAssembler hotbitAssembler;

    @Override
    public Flux<HotbitOpenOrderResponseDto> getOpenOrders(String symbol, ApiKey apiKey) {
        var rest = hotbitConfiguration.getRest();
        var request = HotbitOpenOrderRequestDto.builder()
                .symbol(symbol)
                .build();
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.scheme(rest.getScheme())
                        .host(rest.getHost())
                        .path(rest.getOpenOrders())
                        .queryParams(hotbitAssembler.assembleOpenOrdersQueryParams(apiKey, request))
                        .build()
                )
                .retrieve()
                .bodyToFlux(HotbitOpenOrderResponseDto.class);
    }

    @Override
    public Mono<HotbitCancelResponseDto> cancelOrder(HotbitCancelRequestDto cancelRequest, ApiKey apiKey) {
        var rest = hotbitConfiguration.getRest();
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.scheme(rest.getScheme())
                        .host(rest.getHost())
                        .path(rest.getCancelOrders())
                        .queryParams(hotbitAssembler.assembleCancelOrderQueryParams(apiKey, cancelRequest))
                        .build()
                )
                .retrieve().bodyToMono(HotbitCancelResponseDto.class);
    }

    @Override
    public Mono<HotbitRequestOrderResponseDto> requestOrder(HotbitRequestOrderDto orderRequest, ApiKey apiKey) {
        var rest = hotbitConfiguration.getRest();
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.scheme(rest.getScheme())
                        .host(rest.getHost())
                        .path(rest.getRequestOrders())
                        .queryParams(hotbitAssembler.assembleRequestOrderQueryParams(apiKey, orderRequest))
                        .build()
                )
                .retrieve().bodyToMono(HotbitRequestOrderResponseDto.class);

    }

    @Override
    public Exchange getExchange() {
        return Exchange.HOTBIT;
    }
}
