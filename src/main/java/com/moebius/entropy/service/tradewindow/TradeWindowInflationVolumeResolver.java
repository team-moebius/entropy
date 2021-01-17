package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.DummyOrderConfig;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.dto.order.DividedDummyOrderDto;
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
			.map(inflationConfig -> getRandomVolume(inflationConfig, orderPosition))
			.orElse(BigDecimal.ZERO);
	}

	public List<BigDecimal> getDividedVolume(DividedDummyOrderDto orderDto, OrderPosition orderPosition) {
		List<BigDecimal> dividedVolumes = new ArrayList<>();
		BigDecimal inflationVolume = Optional.ofNullable(orderDto)
			.map(DividedDummyOrderDto::getInflationConfig)
			.map(inflationConfig -> getRandomVolume(inflationConfig, orderPosition))
			.orElse(BigDecimal.ZERO);
		int minDividedOrderCount = 0;
		int maxDividedOrderCount = 0;

		if (orderPosition == OrderPosition.ASK) {
			minDividedOrderCount = Optional.ofNullable(orderDto)
				.map(DividedDummyOrderDto::getAskOrderConfig)
				.map(DummyOrderConfig::getMinDividedOrderCount)
				.orElse(0);
			maxDividedOrderCount = Optional.ofNullable(orderDto)
				.map(DividedDummyOrderDto::getAskOrderConfig)
				.map(DummyOrderConfig::getMaxDividedOrderCount)
				.orElse(0);
		} else if (orderPosition == OrderPosition.BID) {
			minDividedOrderCount = Optional.ofNullable(orderDto)
				.map(DividedDummyOrderDto::getBidOrderConfig)
				.map(DummyOrderConfig::getMinDividedOrderCount)
				.orElse(0);
			maxDividedOrderCount = Optional.ofNullable(orderDto)
				.map(DividedDummyOrderDto::getBidOrderConfig)
				.map(DummyOrderConfig::getMaxDividedOrderCount)
				.orElse(0);
		}

		int range = randomUtils.getRandomInteger(minDividedOrderCount, maxDividedOrderCount);

		for (int i = range - 1; i > 0; --i) {
			BigDecimal dividedVolume = randomUtils.getRandomDecimal(0.1f * i, inflationVolume.floatValue(), 2);
			dividedVolumes.add(dividedVolume);
			inflationVolume = inflationVolume.subtract(dividedVolume);
		}
		dividedVolumes.add(inflationVolume);

		return dividedVolumes;
	}

	private BigDecimal getRandomVolume(InflationConfig inflationConfig, OrderPosition orderPosition) {
		return Optional.ofNullable(inflationConfig)
			.map(config -> {
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
}

