package com.moebius.entropy.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class EntropyRandomUtils {
	public BigDecimal getRandomDecimal(float min, float max, int decimalPlaces) {
		return new BigDecimal(Math.random() * (max - min) + min)
			.setScale(decimalPlaces, RoundingMode.HALF_UP);
	}

	public int getRandomInteger(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}
}
