package com.moebius.entropy.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
public class EntropyRandomUtils {
	public BigDecimal getRandomDecimal(float min, float max, int decimalPlaces) {
		return new BigDecimal(Math.random() * (max - min) + min)
				.setScale(decimalPlaces, RoundingMode.HALF_UP);
	}

	public int getRandomInteger(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	public List<BigDecimal> getRandomSlices(BigDecimal value, Integer sliceNumber, int decimalPlaces) {
		int totalNumber = 0;
		List<Integer> randomNumbers = new ArrayList<>(sliceNumber);
		for (int i = 0; i < sliceNumber; i++) {
			int randomInteger = getRandomInteger(5, 10);
			totalNumber += randomInteger;
			randomNumbers.add(randomInteger);
		}

		final int randomNumberDivisor = totalNumber;
		return randomNumbers.stream()
				.map(randomNumber -> BigDecimal.valueOf((double) randomNumber / randomNumberDivisor))
				.map(value::multiply)
				.map(slice -> slice.setScale(decimalPlaces, RoundingMode.HALF_UP))
				.collect(Collectors.toList());
	}

	public int getBoundedRandomInteger(int bound) {
		return ThreadLocalRandom.current().nextInt(bound);
	}
}
