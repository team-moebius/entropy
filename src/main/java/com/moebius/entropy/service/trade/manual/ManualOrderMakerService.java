package com.moebius.entropy.service.trade.manual;

import com.moebius.entropy.domain.*;
import com.moebius.entropy.service.order.OrderService;
import com.moebius.entropy.service.tradewindow.repository.TradeWindowRepository;
import com.moebius.entropy.util.EntropyRandomUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class ManualOrderMakerService {
    private final static int decimalPosition = 2;
    private final EntropyRandomUtils randomUtil;
    private final OrderService orderService;
    private final TradeWindowRepository tradeWindowRepository;

    public Mono<ManualOrderResult> requestManualOrderMaking(ManualOrderMakingRequest request) {
        int division = randomUtil.getRandomInteger(request.getStartRange(), request.getEndRange());
        List<BigDecimal> randomVolumes = divideVolumeWith(request.getRequestedVolume(), division);

        Market market = request.getMarket();
        BigDecimal marketPrice = tradeWindowRepository.getMarketPriceForSymbol(market);
        OrderPosition orderPosition = request.getOrderPosition();

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

    private List<BigDecimal> divideVolumeWith(BigDecimal requestedVolume, int division) {
        BigDecimal remainVolume = requestedVolume;
        List<BigDecimal> dividedVolumes = new ArrayList<>(division);

        for (int i = 1; i < division; i++) {
            BigDecimal randomVolume = randomUtil.getRandomDecimal(0.0f, remainVolume.floatValue(), decimalPosition);
            dividedVolumes.add(randomVolume);
            remainVolume = remainVolume.subtract(randomVolume);
        }
        dividedVolumes.add(remainVolume);

        return dividedVolumes;
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
