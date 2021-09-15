package com.moebius.entropy.util;

import com.moebius.entropy.domain.trade.TradePrice;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpreadWindowResolver {

    private final EntropyRandomUtils randomUtils;

    //TODO: Delete
    public List<BigDecimal> resolvePrices(SpreadWindowResolveRequest request) {
        return Collections.emptyList();
    }

    public List<Pair<BigDecimal, BigDecimal>> resolvePriceMinVolumePair(
        SpreadWindowResolveRequest request) {
        int count = request.getCount();
        BigDecimal minimumVolume = request.getMinimumVolume();
        BigDecimal startPrice = request.getStartPrice();
        BinaryOperator<BigDecimal> operationOnPrice = request.getOperationOnPrice();
        int spreadWindow = request.getSpreadWindow();
        BigDecimal priceUnit = request.getPriceUnit();
        List<TradePrice> previousWindow = request.getPreviousWindow();
        Map<String, TradePrice> existOrderPerWindow = mergeIntoTradeWindow(
            priceUnit, startPrice, spreadWindow, operationOnPrice, previousWindow
        );

        BigDecimal stepPriceRange = priceUnit
            .multiply(BigDecimal.valueOf(spreadWindow));

        int scale = priceUnit.scale();
        return IntStream.range(1, count + 1)
            .mapToObj(BigDecimal::valueOf)
            .map(multiplier -> Pair.of(multiplier, operationOnPrice
                .apply(startPrice, stepPriceRange.multiply(multiplier))))
            .filter(pricePair -> tradeFromExistWindow(existOrderPerWindow, pricePair.getValue())
                .map(tradePrice -> shouldPopulateTradeWindow(tradePrice, minimumVolume))
                .orElse(true)
            )
            .map(pricePair -> tradeFromExistWindow(existOrderPerWindow, pricePair.getValue())
                .map(tradePrice -> Pair.of(tradePrice.getUnitPrice(), tradePrice.getVolume()))
                .orElseGet(() -> {
                    if (spreadWindow == 1) {
                        return Pair.of(pricePair.getValue(), BigDecimal.ZERO);
                    } else {
                        BigDecimal startMultiplier = pricePair.getKey();
                        BigDecimal endMultiplier = startMultiplier.add(BigDecimal.ONE);
                        BigDecimal rangeStartPrice = pricePair.getValue();
                        BigDecimal rangeEndPrice = operationOnPrice.apply(startPrice,
                            stepPriceRange.multiply(endMultiplier));
                        BigDecimal resolvedPrice = randomUtils.getRandomDecimal(rangeStartPrice,
                            rangeEndPrice, scale);
                        return Pair.of(resolvedPrice, BigDecimal.ZERO);
                    }
                }))
            .collect(Collectors.toList());
    }

    private Optional<TradePrice> tradeFromExistWindow(
        Map<String, TradePrice> existOrderPerWindow, BigDecimal startPrice
    ) {
        String key = startPrice.toPlainString();
        if (existOrderPerWindow.containsKey(key)) {
            return Optional.of(existOrderPerWindow.get(key));
        } else {
            return Optional.empty();
        }
    }

    private boolean shouldPopulateTradeWindow(TradePrice tradePrice, BigDecimal minVolume) {
        if (Objects.isNull(tradePrice)) {
            return true;
        }
        return tradePrice.getVolume().compareTo(minVolume) < 0;
    }

    protected Map<String, TradePrice> mergeIntoTradeWindow(
        BigDecimal priceUnit, BigDecimal startPrice, int spreadWindow,
        BinaryOperator<BigDecimal> operationOnPrice,
        List<TradePrice> prices
    ) {
        BigDecimal stepPriceRange = priceUnit.multiply(BigDecimal.valueOf(spreadWindow));
        return prices.stream()
            .collect(Collectors.groupingBy(tradePrice -> {
                //when marketPrice is 11.35 and spreadWindow is 5
                /*
                 * ASK
                 * if price is 11.36, (11.36-11.35-0.01) / 0.01*5 = 0/0.05 = 0 => spreadStartPrice = 11.36
                 * if price is 11.47, (11.47-11.35-0.01) / 0.01*5 = 0.11/0.05 = 2 => spreadStartPrice = (11.35+0.01) + 0.05 * 2 = 11.36+0.10 = 11.46
                 * BID
                 * if price is 11.34, (11.35-11.34) / 0.01*5 = 0.01/0.05 = 0 => spreadStartPrice = 11.35
                 * if price is 11.15, (11.35-11.15) / 0.01*5 = 0.20/0.05 = 4 => spreadStartPrice = 11.35 - 0.05 * 4 = 11.35-0.20 = 11.15
                 */
                BigDecimal price = tradePrice.getUnitPrice();
                BigDecimal spreadUnitStep = startPrice.subtract(price).abs()
                    .divide(stepPriceRange, 0, RoundingMode.FLOOR);

                BigDecimal spreadStartPrice = operationOnPrice.apply(
                    startPrice,
                    stepPriceRange.multiply(spreadUnitStep)
                );

                return spreadStartPrice.toPlainString();
            }))
            .entrySet().stream()
            .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
            .map(tradePricesPair -> {
                String startPriceString = tradePricesPair.getKey();
                List<TradePrice> tradePricesByWindow = tradePricesPair.getValue();
                TradePrice maxVolumePrice = tradePricesByWindow.stream()
                    .max(Comparator.comparing(TradePrice::getVolume))
                    .orElse(tradePricesByWindow.get(0));
                return Pair.of(startPriceString, maxVolumePrice);
            })
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }
}
