package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflateRequest;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.inflate.InflationResult;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.InflationConfigRepository;
import com.moebius.entropy.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeWindowInflateService {

    private static final int START_FROM_MARKET_PRICE = 0;
    private static final int START_FROM_NEXT_PRICE = 1;

    private final TradeWindowQueryService tradeWindowQueryService;
    private final InflationConfigRepository inflationConfigRepository;
    private final OrderService orderService;
    private final TradeWindowInflationVolumeResolver volumeResolver;
    private final BobooTradeWindowChangeEventListener windowChangeEventListener;

    @SuppressWarnings("unused")
    @PostConstruct
    public void onCreate() {
        windowChangeEventListener.setTradeWindowInflateService(this);
}

    public Mono<InflationResult> inflateTrades(InflateRequest inflateRequest) {
        Market market = inflateRequest.getMarket();
        InflationConfig inflationConfig = inflationConfigRepository.getConfigFor(market);
        if (!inflationConfig.isEnable()) {
            return Mono.empty();
        }

        return tradeWindowQueryService.fetchTradeWindow(market)
            .flatMap(tradeWindow -> {
                Flux<Order> createdOrders = generateRequiredOrderRequest(market, tradeWindow,
                        inflationConfig)
                        .flatMap(orderService::requestOrder)
                        .onErrorContinue((throwable, orderRequest) -> log.warn(
                                "[TradeWindowInflation] Failed to request Order with {}", orderRequest, throwable
                        ));

                Flux<Order> cancelledOrders = cancelInvalidOrders(market, inflationConfig);

                return collectResult(createdOrders, cancelledOrders);
            })
            .switchIfEmpty(Mono.empty());
    }

    private Flux<OrderRequest> generateRequiredOrderRequest(
        Market market, TradeWindow window, InflationConfig inflationConfig
    ) {

        Flux<OrderRequest> bidRequestFlux = makeOrderRequestWith(
            START_FROM_MARKET_PRICE, inflationConfig.getBidCount(), window.getBidPrices(), market,
            OrderPosition.BID, BigDecimal::subtract
        );

        Flux<OrderRequest> askRequestFlux = makeOrderRequestWith(
            START_FROM_NEXT_PRICE, inflationConfig.getAskCount(), window.getAskPrices(), market,
            OrderPosition.ASK, BigDecimal::add
        );
        return Flux.merge(bidRequestFlux, askRequestFlux);
    }

    private Flux<OrderRequest> makeOrderRequestWith(
        int startFrom, int count, List<TradePrice> prices, Market market,
        OrderPosition orderPosition, BinaryOperator<BigDecimal> priceCalculationHandler
    ) {
        BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
        BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();
        Set<BigDecimal> priceSet = prices.stream()
            .map(TradePrice::getUnitPrice)
            .collect(Collectors.toSet());

        return Flux.range(startFrom, count)
            .map(BigDecimal::valueOf)
            .map(multiplier -> priceCalculationHandler
                .apply(marketPrice, priceUnit.multiply(multiplier)))
            .filter(price -> !priceSet.contains(price))
            .map(price -> {
                BigDecimal inflationVolume = volumeResolver
                    .getInflationVolume(market, orderPosition);
                return new OrderRequest(market, orderPosition, price, inflationVolume);
            });
    }


    private Flux<Order> cancelInvalidOrders(Market market, InflationConfig inflationConfig) {
        BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
        BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

        BigDecimal maxAskPrice = marketPrice.add(
            priceUnit
                .multiply(BigDecimal.valueOf(inflationConfig.getAskCount() + START_FROM_NEXT_PRICE))
        );

        BigDecimal minBidPrice = marketPrice.subtract(
            priceUnit.multiply(BigDecimal.valueOf(inflationConfig.getBidCount()))
        );

        return orderService.fetchAutomaticOrdersFor(market)
                .filter(order -> {
                    BigDecimal orderPrice = order.getPrice();
                    if (OrderPosition.ASK.equals(order.getOrderPosition())) {
                        return orderPrice.compareTo(maxAskPrice) > 0;
                    } else {
                        return orderPrice.compareTo(minBidPrice) < 0;
                    }
                })
                .flatMap(orderService::cancelOrder)
                .onErrorContinue((throwable, order) -> log.warn(
                        "[TradeWindowInflation] Failed to cancel Order {}", order, throwable
                ));
    }

    private Mono<InflationResult> collectResult(Flux<Order> createdOrders,
        Flux<Order> cancelledOrders) {
        return Mono.zip(
            createdOrders.collectMultimap(Order::getOrderPosition, Order::getPrice),
            cancelledOrders.collectMultimap(Order::getOrderPosition, Order::getPrice)
        ).map(resultsTuple -> {
            Map<OrderPosition, Collection<BigDecimal>> createdOrderByType = resultsTuple.getT1();
            Map<OrderPosition, Collection<BigDecimal>> cancelledOrderByType = resultsTuple.getT2();
            return new InflationResult(
                getPrices(createdOrderByType, OrderPosition.ASK),
                getPrices(createdOrderByType, OrderPosition.BID),
                getPrices(cancelledOrderByType, OrderPosition.ASK),
                getPrices(cancelledOrderByType, OrderPosition.BID)
            );
        });
    }

    private List<BigDecimal> getPrices(
        Map<OrderPosition, Collection<BigDecimal>> pricesByType, OrderPosition orderPosition
    ) {
        return new ArrayList<>(
            pricesByType.getOrDefault(orderPosition, Collections.emptyList())
        );

    }
}
