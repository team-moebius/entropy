package com.moebius.entropy.dto.exchange.order.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moebius.entropy.entity.order.OrderType;
import com.moebius.entropy.entity.order.PriceType;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BobooOrderDto {
	private String symbol;
	private float price;
	private float leverage;
	private float originalQuantity;
	private float executedQuantity;
	private float averagePrice;
	private OrderType orderType;
	private PriceType priceType;
	private String fees;
	private long createdAt;
	private long updatedAt;
}
