package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.InflateRequest;
import com.moebius.entropy.domain.InflationConfig;
import com.moebius.entropy.domain.InflationResult;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.Order;
import com.moebius.entropy.domain.OrderRequest;
import com.moebius.entropy.domain.OrderType;
import com.moebius.entropy.domain.TradePrice;
import com.moebius.entropy.domain.TradeWindow;
import com.moebius.entropy.service.tradewindow.repository.InflationConfigRepository;
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

@RequiredArgsConstructor
@Service
public class TradeWindowInflateService {

    private final TradeWindowService tradeWindowService;
    private final InflationConfigRepository inflationConfigRepository;
    private final OrderService orderService;

    public Mono<InflationResult> inflateTrades(InflateRequest inflateRequest) {
        Market market = inflateRequest.getMarket();
        InflationConfig inflationConfig = inflationConfigRepository.getConfigFor(market);

        return tradeWindowService.fetchTradeWindow(market)
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
        String symbol = market.getSymbol();
        Exchange exchange = market.getExchange();
        BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();
        BigDecimal fallbackStartPrice = tradeWindowService.getMarketPrice(market);

        Flux<Order> askOrders = makeOrdersWith(
            symbol, exchange, OrderType.ASK, tradeWindow.getAskPrices(),
            priceUnit, fallbackStartPrice, inflationConfig.getAskCount(),
            BigDecimal::subtract
        );

        Flux<Order> bidOrders = makeOrdersWith(
            symbol, exchange, OrderType.BID, tradeWindow.getBidPrices(),
            priceUnit, fallbackStartPrice, inflationConfig.getBidCount(),
            BigDecimal::add
        );

        return askOrders.concatWith(bidOrders);
    }

    private Flux<Order> makeOrdersWith(
        String symbol, Exchange exchange, OrderType orderType, List<TradePrice> prices,
        BigDecimal priceUnit, BigDecimal fallbackStartPrice,
        int countObjective, BinaryOperator<BigDecimal> priceCalculationHandler
    ) {
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
            .map(price -> new OrderRequest(symbol, exchange, orderType, price))
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
                if (OrderType.ASK.equals(order.getOrderType())) {
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
            createdOrders.collectMultimap(Order::getOrderType, Order::getPrice),
            cancelledOrders.collectMultimap(Order::getOrderType, Order::getPrice)
        ).map(resultsTuple -> {
            Map<OrderType, Collection<BigDecimal>> createdOrderByType = resultsTuple.getT1();
            Map<OrderType, Collection<BigDecimal>> cancelledOrderByType = resultsTuple.getT2();
            return new InflationResult(
                getPrices(createdOrderByType, OrderType.ASK),
                getPrices(createdOrderByType, OrderType.BID),
                getPrices(cancelledOrderByType, OrderType.ASK),
                getPrices(cancelledOrderByType, OrderType.BID)
            );
        });
    }

    private List<BigDecimal> getPrices(
        Map<OrderType, Collection<BigDecimal>> pricesByType, OrderType orderType
    ) {
        return new ArrayList<>(
            pricesByType.getOrDefault(orderType, Collections.emptyList())
        );

    }
}
