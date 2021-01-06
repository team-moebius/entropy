package com.moebius.entropy.service.order;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class OrderQuantityService {
	public BigDecimal getRandomQuantity(float min, float max, int decimalPlaces) {
		return new BigDecimal(Math.random() * (max - min) + min)
			.setScale(decimalPlaces, RoundingMode.HALF_UP);
	}
}
