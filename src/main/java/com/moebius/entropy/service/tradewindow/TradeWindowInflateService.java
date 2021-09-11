package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflateRequest;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.InflationConfigRepository;
import com.moebius.entropy.service.order.OrderService;
import com.moebius.entropy.service.order.OrderServiceFactory;
import com.moebius.entropy.util.SpreadWindowResolver;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                Flux<Order> requestOrderFlux = requestRequiredOrders(market, tradeWindow,
                    inflationConfig);
                Flux<Order> cancelOrderFlux = cancelInvalidOrders(market, inflationConfig);

                return Flux.merge(requestOrderFlux, cancelOrderFlux);
            })
            .onErrorContinue(
                (throwable, o) -> log.warn("[TradeWindowInflation] Failed to collect order result.",
                    throwable));
    }

    private Flux<Order> requestRequiredOrders(Market market, TradeWindow window,
        InflationConfig inflationConfig) {
        BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
        BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

        BigDecimal bidStartPrice = marketPrice.subtract(priceUnit);
        Map<String, BigDecimal> bidVolumeBySpreadWindow = spreadWindowResolver.mergeIntoTradeWindow(
            market, bidStartPrice, inflationConfig.getSpreadWindow(), BigDecimal::subtract,
            window.getBidPrices()
        );
        Flux<OrderRequest> bidRequestFlux = makeOrderRequestWith(
            inflationConfig.getBidCount(), inflationConfig.getBidMinVolume(), market, bidStartPrice,
            OrderPosition.BID, BigDecimal::subtract, inflationConfig.getSpreadWindow(),
            bidVolumeBySpreadWindow);

        BigDecimal askStartPrice = marketPrice.add(priceUnit);
        Map<String, BigDecimal> askVolumeBySpreadWindow = spreadWindowResolver.mergeIntoTradeWindow(
            market, askStartPrice, inflationConfig.getSpreadWindow(), BigDecimal::add,
            window.getAskPrices()
        );
        Flux<OrderRequest> askRequestFlux = makeOrderRequestWith(
            inflationConfig.getAskCount(), inflationConfig.getAskMinVolume(), market, askStartPrice,
            OrderPosition.ASK, BigDecimal::add, inflationConfig.getSpreadWindow(),
            askVolumeBySpreadWindow);

        OrderService orderService = orderServiceFactory.getOrderService(market.getExchange());

        return Flux.merge(bidRequestFlux, askRequestFlux)
            .doOnNext(
                orderRequest -> log.info("[TradeWindowInflation] Create order for inflation. [{}]",
                    orderRequest))
            .flatMap(orderService::requestOrder)
            .onErrorContinue(
                (throwable, o) -> log.warn("[TradeWindowInflation] Failed to request order.",
                    throwable));
    }

    private Flux<OrderRequest> makeOrderRequestWith(
        int count, BigDecimal minimumVolume, Market market, BigDecimal startPrice,
        OrderPosition orderPosition, BinaryOperator<BigDecimal> operationOnPrice, int spreadWindow,
        Map<String, BigDecimal> volumesBySpreadWindow
    ) {
        BigDecimal priceUnit = market
            .getTradeCurrency()
            .getPriceUnit();

        List<BigDecimal> resolvedPriceWindow = spreadWindowResolver.resolvePrices(
            count, minimumVolume, startPrice, operationOnPrice,
            spreadWindow, priceUnit, volumesBySpreadWindow
        );

        return Flux.fromIterable(resolvedPriceWindow)
            .map(price -> {
                BigDecimal inflationVolume = volumeResolver.getInflationVolume(market,
                    orderPosition);
                return new OrderRequest(market, orderPosition, price, inflationVolume);
            });
    }


    private Flux<Order> cancelInvalidOrders(Market market, InflationConfig inflationConfig) {
        BigDecimal marketPrice = tradeWindowQueryService.getMarketPrice(market);
        BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();

        int spreadWindow = inflationConfig.getSpreadWindow();
        BigDecimal priceRangeForSteps = priceUnit.multiply(BigDecimal.valueOf(spreadWindow));

        BigDecimal askStartPrice = marketPrice.add(priceUnit);
        BigDecimal maxAskPrice = askStartPrice.add(
            priceRangeForSteps.multiply(BigDecimal.valueOf(inflationConfig.getAskCount()))
        );

        BigDecimal bidStartPrice = marketPrice.subtract(priceUnit);
        BigDecimal minBidPrice = bidStartPrice.subtract(
            priceRangeForSteps.multiply(BigDecimal.valueOf(inflationConfig.getBidCount()))
        );

        OrderService orderService = orderServiceFactory.getOrderService(market.getExchange());

        return orderService.fetchAllOrdersFor(market)
            .filter(order -> {
                BigDecimal orderPrice = order.getPrice();
                if (OrderPosition.ASK.equals(order.getOrderPosition())) {
                    return orderPrice.compareTo(maxAskPrice) > 0;
                } else {
                    return orderPrice.compareTo(minBidPrice) < 0;
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
