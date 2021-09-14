package com.moebius.entropy.util;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradePrice;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpreadWindowResolver {

    private final EntropyRandomUtils randomUtils;

    public List<BigDecimal> resolvePrices(SpreadWindowResolveRequest request){
        int count = request.getCount();
        BigDecimal minimumVolume = request.getMinimumVolume();
        BigDecimal startPrice = request.getStartPrice();
        BinaryOperator<BigDecimal> operationOnPrice = request.getOperationOnPrice();
        int spreadWindow = request.getSpreadWindow();
        BigDecimal priceUnit = request.getPriceUnit();
        Map<String, BigDecimal> previousWindow = request.getPreviousWindow();

        BigDecimal stepPriceRange = priceUnit
            .multiply(BigDecimal.valueOf(spreadWindow));

        int scale = priceUnit.scale();
        return IntStream.range(1, count + 1)
            .mapToObj(BigDecimal::valueOf)
            .map(multiplier -> Pair.of(multiplier, operationOnPrice
                .apply(startPrice, stepPriceRange.multiply(multiplier))))
            .filter(pricePair ->
                previousWindow.getOrDefault(
                    pricePair.getValue().toPlainString(), BigDecimal.ZERO
                ).compareTo(minimumVolume) < 0)
            .map(pricePair -> {
                if (spreadWindow == 1) {
                    return pricePair.getValue();
                }
                BigDecimal startMultiplier = pricePair.getKey();
                BigDecimal endMultiplier = startMultiplier.add(BigDecimal.ONE);
                BigDecimal rangeStartPrice = pricePair.getValue();
                BigDecimal rangeEndPrice = operationOnPrice.apply(startPrice,
                    stepPriceRange.multiply(endMultiplier));
                return randomUtils.getRandomDecimal(rangeStartPrice, rangeEndPrice, scale);
            })
            .collect(Collectors.toList());
    }

    public Map<String, BigDecimal> mergeIntoTradeWindow(
        Market market, BigDecimal startPrice, int spreadWindow,
        BinaryOperator<BigDecimal> operationOnPrice,
        List<TradePrice> prices
    ) {
        BigDecimal priceUnit = market.getTradeCurrency().getPriceUnit();
        BigDecimal stepPriceRange = priceUnit.multiply(BigDecimal.valueOf(spreadWindow));

        Map<String, BigDecimal> volumesBySpreadWindows = new HashMap<>();

        prices.forEach(tradePrice -> {
            BigDecimal price = tradePrice.getUnitPrice();

            //when marketPrice is 11.35 and spreadWindow is 5
            /*
             * ASK
             * if price is 11.36, (11.36-11.35-0.01) / 0.01*5 = 0/0.05 = 0 => spreadStartPrice = 11.36
             * if price is 11.47, (11.47-11.35-0.01) / 0.01*5 = 0.11/0.05 = 2 => spreadStartPrice = (11.35+0.01) + 0.05 * 2 = 11.36+0.10 = 11.46
             * BID
             * if price is 11.34, (11.35-11.34) / 0.01*5 = 0.01/0.05 = 0 => spreadStartPrice = 11.35
             * if price is 11.15, (11.35-11.15) / 0.01*5 = 0.20/0.05 = 4 => spreadStartPrice = 11.35 - 0.05 * 4 = 11.35-0.20 = 11.15
             */

            BigDecimal spreadUnitStep = startPrice.subtract(price).abs()
                .divide(stepPriceRange, 0, RoundingMode.FLOOR);

            BigDecimal spreadStartPrice = operationOnPrice.apply(
                startPrice,
                stepPriceRange.multiply(spreadUnitStep)
            );

            String startPriceString = spreadStartPrice.toPlainString();
            BigDecimal volumeBySpreadWindow = volumesBySpreadWindows.getOrDefault(startPriceString,
                    BigDecimal.ZERO)
                .add(tradePrice.getVolume());
            volumesBySpreadWindows.put(startPriceString, volumeBySpreadWindow);
        });
        return volumesBySpreadWindows;
    }
}
