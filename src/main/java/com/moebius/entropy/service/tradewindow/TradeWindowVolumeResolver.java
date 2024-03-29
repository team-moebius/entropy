package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.domain.order.config.DummyOrderConfig;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.dto.MarketDto;
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
public class TradeWindowVolumeResolver {
	private final InflationConfigRepository inflationConfigRepository;
	private final EntropyRandomUtils randomUtils;

	public BigDecimal getRandomMarketVolume(BigDecimal minVolume, BigDecimal maxVolume, int decimalPosition) {
		return randomUtils.getRandomDecimal(minVolume.floatValue(), maxVolume.floatValue(), decimalPosition);
	}

	public BigDecimal getInflationVolume(Market market, OrderPosition orderPosition) {
		return Optional.ofNullable(inflationConfigRepository.getConfigFor(market))
			.map(inflationConfig -> getRandomVolume(inflationConfig, orderPosition, market.getVolumeDecimalPosition()))
			.orElse(BigDecimal.ZERO);
	}

	public List<BigDecimal> getDividedVolume(DividedDummyOrderDto orderDto, OrderPosition orderPosition) {
		List<BigDecimal> dividedVolumes = new ArrayList<>();
		BigDecimal inflationVolume = Optional.ofNullable(orderDto)
			.map(DividedDummyOrderDto::getInflationConfig)
			.map(inflationConfig -> getRandomVolume(inflationConfig, orderPosition, orderDto.getMarket().getVolumeDecimalPosition()))
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
			BigDecimal dividedVolume = randomUtils.getRandomDecimal(1f * i, inflationVolume.floatValue(), Optional.ofNullable(orderDto)
				.map(DividedDummyOrderDto::getMarket)
				.map(MarketDto::getVolumeDecimalPosition)
				.orElse(0));
			dividedVolumes.add(dividedVolume);
			inflationVolume = inflationVolume.subtract(dividedVolume);
		}

		if (inflationVolume.compareTo(BigDecimal.ONE) < 0) {
			dividedVolumes.add(BigDecimal.ONE);
		} else {
			dividedVolumes.add(inflationVolume);
		}

		return dividedVolumes;
	}

	private BigDecimal getRandomVolume(InflationConfig inflationConfig, OrderPosition orderPosition, int decimalPosition) {
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

