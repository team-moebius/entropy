package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflateRequest;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.repository.InflationConfigRepository;
import com.moebius.entropy.service.order.OrderService;
import com.moebius.entropy.service.order.OrderServiceFactory;
import com.moebius.entropy.util.SpreadWindowResolveRequest;
import com.moebius.entropy.util.SpreadWindowResolver;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeWindowInflateService {

    private final TradeWindowQueryService tradeWindowQueryService;
    private final InflationConfigRepository inflationConfigRepository;
    private final OrderServiceFactory orderServiceFactory;
    private final TradeWindowVolumeResolver volumeResolver;
    private final SpreadWindowResolver spreadWindowResolver;

    public Flux<Order> inflateOrders(InflateRequest inflateRequest) {
        Market targetMarket = inflateRequest.getMarket();
        InflationConfig inflationConfig = inflationConfigRepository.getConfigFor(targetMarket);
        Market market = Optional.ofNullable(inflationConfig.getMarket())
            .orElse(targetMarket);
        if (!inflationConfig.isEnable()) {
            return Flux.empty();
        }

        return tradeWindowQueryService.getTradeWindowMono(market)
            .flatMapMany(tradeWindow -> {
                List<TradePrice> askWindow = tradeWindow.getAskPrices();

                List<Pair<BigDecimal, BigDecimal>> newAskInflation = resolveSpreadWindow(market,
                    OrderPosition.ASK, inflationConfig, askWindow);

                Flux<Order> askRequestOrderFlux = requestRequiredOrders(
                    market, OrderPosition.ASK, newAskInflation
                );

                List<TradePrice> bidWindow = tradeWindow.getBidPrices();

                List<Pair<BigDecimal, BigDecimal>> newBidInflation = resolveSpreadWindow(market,
                    OrderPosition.BID, inflationConfig, bidWindow);

                Flux<Order> bidRequestOrderFlux = requestRequiredOrders(
                    market, OrderPosition.BID, newBidInflation
                );

                Flux<Order> cancelOrderFlux = cancelInvalidOrders(
                    market, newAskInflation, newBidInflation
                );

                return Flux.merge(cancelOrderFlux, askRequestOrderFlux, bidRequestOrderFlux);
            })
            .onErrorContinue(
                (throwable, o) -> log.warn("[TradeWindowInflation] Failed to collect order result.",
                    throwable));
    }

    private List<Pair<BigDecimal, BigDecimal>> resolveSpreadWindow(
        Market market, OrderPosition orderPosition,
        InflationConfig inflationConfig, List<TradePrice> tradeWindow
    ) {
        BigDecimal priceUnit = market
            .getTradeCurrency()
            .getPriceUnit();
        BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);

        BigDecimal minimumVolume;
        BigDecimal startPrice;
        BinaryOperator<BigDecimal> operationOnPrice;
        int count;
        int spreadWindow = inflationConfig.getSpreadWindow();
        if (OrderPosition.ASK.equals(orderPosition)) {
            count = inflationConfig.getAskCount();
            minimumVolume = inflationConfig.getAskMinVolume();
            operationOnPrice = BigDecimal::add;
            startPrice = marketPrice.add(priceUnit);
        } else {
            count = inflationConfig.getBidCount();
            minimumVolume = inflationConfig.getBidMinVolume();
            operationOnPrice = BigDecimal::subtract;
            startPrice = marketPrice.subtract(priceUnit);
        }

        SpreadWindowResolveRequest request = SpreadWindowResolveRequest.builder()
            .count(count)
            .minimumVolume(minimumVolume)
            .startPrice(startPrice)
            .operationOnPrice(operationOnPrice)
            .spreadWindow(spreadWindow)
            .priceUnit(priceUnit)
            .previousWindow(tradeWindow)
            .build();

        return spreadWindowResolver.resolvePriceMinVolumePair(request);
    }

    private Flux<Order> requestRequiredOrders(
        Market market, OrderPosition orderPosition,
        List<Pair<BigDecimal, BigDecimal>> resolvedInflation
    ) {
        OrderService orderService = orderServiceFactory.getOrderService(market.getExchange());

        return Flux.fromIterable(resolvedInflation)
            .filter(priceVolumePair -> priceVolumePair.getValue().compareTo(BigDecimal.ZERO) >= 0)
            .map(priceVolumePair -> {
                BigDecimal price = priceVolumePair.getKey();
                BigDecimal deductiveVolume = priceVolumePair.getValue();

                BigDecimal randomVolume = volumeResolver.getInflationVolume(market,
                    orderPosition);
                BigDecimal inflationVolume = randomVolume.subtract(deductiveVolume);
                return new OrderRequest(market, orderPosition, price, inflationVolume);
            })
            .doOnNext(
                orderRequest -> log.info("[TradeWindowInflation] Create order for inflation. [{}]",
                    orderRequest))
            .flatMap(orderService::requestOrder)
            .onErrorContinue(
                (throwable, o) -> log.warn("[TradeWindowInflation] Failed to request order.",
                    throwable));
    }

    private Flux<Order> cancelInvalidOrders(Market market,
        List<Pair<BigDecimal, BigDecimal>> newAskInflation,
        List<Pair<BigDecimal, BigDecimal>> newBidInflation
    ) {
        Set<String> validAskPrices = newAskInflation.stream()
            .map(Pair::getKey)
            .map(BigDecimal::toPlainString)
            .collect(Collectors.toSet());
        Set<String> validBidPrices = newBidInflation.stream()
            .map(Pair::getKey)
            .map(BigDecimal::toPlainString)
            .collect(Collectors.toSet());

        OrderService orderService = orderServiceFactory.getOrderService(market.getExchange());

        return orderService.fetchAllOrdersFor(market)
            .filter(order -> {
                BigDecimal orderPrice = order.getPrice();
                String priceKey = orderPrice.toPlainString();
                if (OrderPosition.ASK.equals(order.getOrderPosition())) {
                    return !validAskPrices.contains(priceKey);
                } else {
                    return !validBidPrices.contains(priceKey);
                }
            })
            .doOnNext(
                order -> log.info("[TradeWindowInflation] Cancel order for inflation. [{}]", order))
            .flatMap(orderService::cancelOrder)
            .onErrorContinue(
                (throwable, o) -> log.warn("[TradeWindowInflation] Failed to cancel order.",
                    throwable));
    }
}
