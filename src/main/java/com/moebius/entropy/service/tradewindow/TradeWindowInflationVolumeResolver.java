package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.repository.InflationConfigRepository;
import com.moebius.entropy.util.EntropyRandomUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TradeWindowInflationVolumeResolver {
	private final InflationConfigRepository inflationConfigRepository;
	private final EntropyRandomUtils randomUtils;
	private final static int decimalPosition = 2;

	public BigDecimal getInflationVolume(Market market, OrderPosition orderPosition) {
		return Optional.ofNullable(inflationConfigRepository.getConfigFor(market))
			.map(inflationConfig -> {
				BigDecimal maxVolume;
				BigDecimal minVolume;
				if (OrderPosition.ASK.equals(orderPosition)) {
					maxVolume = inflationConfig.getAskMaxVolume();
					minVolume = inflationConfig.getAskMinVolume();
				} else {
					maxVolume = inflationConfig.getBidMaxVolume();
					minVolume = inflationConfig.getBidMinVolume();
				}
				return Pair.of(minVolume, maxVolume);
			})
			.map(rangePair -> randomUtils.getRandomDecimal(
				rangePair.getLeft().floatValue(), rangePair.getRight().floatValue(), decimalPosition
			))
			.orElse(BigDecimal.ZERO);
	}

	public List<BigDecimal> getDividedVolume(Market market, OrderPosition orderPosition, int minDividedOrderCount, int maxDividedOrderCount) {
		List<BigDecimal> dividedVolumes = new ArrayList<>();
		BigDecimal inflationVolume = getInflationVolume(market, orderPosition);
		int range = randomUtils.getRandomInteger(minDividedOrderCount, maxDividedOrderCount);

		for (int i = range - 1; i > 0; --i) {
			BigDecimal dividedVolume = randomUtils.getRandomDecimal(0.1f * i, inflationVolume.floatValue(), 2);
			dividedVolumes.add(dividedVolume);
			inflationVolume = inflationVolume.subtract(dividedVolume);
		}
		dividedVolumes.add(inflationVolume);

		return dividedVolumes;
	}
}

