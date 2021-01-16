package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.inflate.InflateRequest;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.inflate.InflationResult;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.trade.TradeCurrency;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.service.order.OrderService;
import com.moebius.entropy.repository.InflationConfigRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class TradeWindowInflateService {

    private final TradeWindowQueryService tradeWindowQueryService;
    private final InflationConfigRepository inflationConfigRepository;
    private final OrderService orderService;
    private final TradeWindowInflationVolumeResolver volumeResolver;
    private final BobooTradeWindowChangeEventListener windowChangeEventListener;

    @PostConstruct
    public void onCreate(){
        windowChangeEventListener.setTradeWindowInflateService(this);
        inflationConfigRepository.saveConfigFor(new Market(Exchange.BOBOO, "GTAXUSDT", TradeCurrency.USDT), InflationConfig.builder()
            .askCount(10)
            .bidCount(10)
            .askMinVolume(BigDecimal.valueOf(10.5))
            .askMaxVolume(BigDecimal.valueOf(100.38))
            .bidMinVolume(BigDecimal.valueOf(10.5))
            .bidMaxVolume(BigDecimal.valueOf(100.5))
            .build());
}

    public Mono<InflationResult> inflateTrades(InflateRequest inflateRequest) {
        Market market = inflateRequest.getMarket();
        InflationConfig inflationConfig = inflationConfigRepository.getConfigFor(market);

        return tradeWindowQueryService.fetchTradeWindow(market)
            .flatMap(tradeWindow -> {
                Flux<Order> createdOrders = Mono.just(tradeWindow)
                    .filter(window -> shouldInflateTrade(window, inflationConfig))
                    .flatMapMany(window -> makeOrderInflation(window, market, inflationConfig))
                    .switchIfEmpty(Flux.empty());

                Flux<Order> cancelledOrders = cancelPreviousOrders(tradeWindow, market,
                    inflationConfig);

                return collectResult(createdOrders, cancelledOrders);
            })
            .switchIfEmpty(Mono.empty());
    }

    private boolean shouldInflateTrade(TradeWindow tradeWindow, InflationConfig inflationConfig) {
        return inflationConfig.getBidCount() > tradeWindow.getBidPrices().size() ||
            inflationConfig.getAskCount() > tradeWindow.getAskPrices().size();
    }

    private Flux<Order> makeOrderInflation(
        TradeWindow tradeWindow, Market market, InflationConfig inflationConfig
    ) {
        BigDecimal fallbackStartPrice = tradeWindowQueryService.getMarketPrice(market);

        Flux<Order> askOrders = makeOrdersWith(
            market, OrderPosition.ASK, tradeWindow.getAskPrices(), fallbackStartPrice,
            inflationConfig.getAskCount(), BigDecimal::subtract
        );

        Flux<Order> bidOrders = makeOrdersWith(
            market, OrderPosition.BID, tradeWindow.getBidPrices(), fallbackStartPrice,
            inflationConfig.getBidCount(), BigDecimal::add
        );

        return askOrders.concatWith(bidOrders);
    }

    private Flux<Order> makeOrdersWith(
        Market market, OrderPosition orderPosition, List<TradePrice> prices,
        BigDecimal fallbackStartPrice,
        int countObjective, BinaryOperator<BigDecimal> priceCalculationHandler
    ) {
        BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();
        int tradeWindowSize = prices.size();

        BigDecimal startPrice = Optional.of(prices)
            .filter(tradePrices -> !tradePrices.isEmpty())
            .map(tradePrices -> tradePrices.get(prices.size() - 1))
            .map(TradePrice::getUnitPrice)
            .orElse(fallbackStartPrice);

        return Flux.fromIterable(
            Optional.of(countObjective - tradeWindowSize)
                .filter(askOrderInflationCount -> askOrderInflationCount > 0)
                .map(askOrderInflationCount -> IntStream.rangeClosed(1, askOrderInflationCount)
                    .mapToObj(BigDecimal::valueOf)
                    .map(multiplier -> priceCalculationHandler
                        .apply(startPrice, priceUnit.multiply(multiplier)))
                    .collect(Collectors.toList())
                )
                .orElse(Collections.emptyList()))
            .map(price -> {
                BigDecimal inflationVolume = volumeResolver.getInflationVolume(market, orderPosition);
                return new OrderRequest(market, orderPosition, price, inflationVolume);
            })
            .flatMap(orderService::requestOrder);
    }


    private Flux<Order> cancelPreviousOrders(
        TradeWindow tradeWindow, Market market, InflationConfig inflationConfig
    ) {
        int askCountObjective = inflationConfig.getAskCount();
        Set<BigDecimal> cancellableAskPriceSet = Optional.ofNullable(tradeWindow.getAskPrices())
            .filter(prices -> prices.size() > askCountObjective)
            .map(prices -> prices.subList(askCountObjective, prices.size()))
            .map(prices -> prices.stream()
                .map(TradePrice::getUnitPrice)
                .collect(Collectors.toSet())
            )
            .orElse(Collections.emptySet());

        int bidCountObjective = inflationConfig.getBidCount();
        Set<BigDecimal> cancellableBidPriceSet = Optional.ofNullable(tradeWindow.getBidPrices())
            .filter(prices -> prices.size() > bidCountObjective)
            .map(prices -> prices.subList(bidCountObjective, prices.size()))
            .map(prices -> prices.stream()
                .map(TradePrice::getUnitPrice)
                .collect(Collectors.toSet())
            )
            .orElse(Collections.emptySet());

        return orderService.fetchAutomaticOrdersFor(market)
            .filter(order -> {
                if (OrderPosition.ASK.equals(order.getOrderPosition())) {
                    return cancellableAskPriceSet.contains(order.getPrice());
                } else {
                    return cancellableBidPriceSet.contains(order.getPrice());
                }
            })
            .flatMap(orderService::cancelOrder);
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
