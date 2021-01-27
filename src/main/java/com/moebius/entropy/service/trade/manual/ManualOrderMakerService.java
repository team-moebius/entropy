package com.moebius.entropy.service.trade.manual;

import com.moebius.entropy.domain.ManualOrderMakingRequest;
import com.moebius.entropy.domain.ManualOrderResult;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.repository.TradeWindowRepository;
import com.moebius.entropy.service.order.OrderService;
import com.moebius.entropy.util.EntropyRandomUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ManualOrderMakerService {
    private final static int decimalPosition = 2;
    private final EntropyRandomUtils randomUtil;
    private final OrderService orderService;
    private final TradeWindowRepository tradeWindowRepository;

    public Mono<ManualOrderResult> requestManualOrderMaking(ManualOrderMakingRequest request) {
        int division = randomUtil.getRandomInteger(request.getStartRange(), request.getEndRange());
        List<BigDecimal> randomVolumes = randomUtil.getRandomSlices(request.getRequestedVolume(), division, decimalPosition);

        Market market = request.getMarket();
        BigDecimal marketPrice = tradeWindowRepository.getMarketPriceForSymbol(market);
        OrderPosition orderPosition = request.getOrderPosition();

        log.info("[ManualOrder] Started to request Order symbol:{}{}, position: {}, quantities:{}",
                market.getSymbol(), market.getTradeCurrency(), orderPosition, randomVolumes);

        return Flux.fromIterable(randomVolumes)
                .map(volume -> new OrderRequest(market, orderPosition, marketPrice, volume))
                .subscribeOn(Schedulers.parallel())
                .flatMap(orderService::requestManualOrder)
                .flatMap(order -> {
                    if (order.getVolume().compareTo(BigDecimal.ZERO) > 0) {
                        return orderService.cancelOrder(order)
                                .map(cancelledOrder -> Pair.of(order, cancelledOrder));
                    } else {
                        return Mono.just(Pair.of(order, null));
                    }
                })
                .collectList()
                .map(this::makeResult);
    }

    private ManualOrderResult makeResult(List<Pair<Order, ?>> pairs) {
        List<Order> requestedOrders = pairs.stream()
                .map(Pair::getLeft)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Order> cancelledOrders = pairs.stream()
                .map(Pair::getRight)
                .filter(Objects::nonNull)
                .map(object -> (Order) object)
                .collect(Collectors.toList());

        return ManualOrderResult.builder()
                .requestedOrders(requestedOrders)
                .cancelledOrders(cancelledOrders)
                .build();
    }
}
