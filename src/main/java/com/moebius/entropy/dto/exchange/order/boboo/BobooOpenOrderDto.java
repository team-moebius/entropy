package com.moebius.entropy.dto.exchange.order.boboo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moebius.entropy.domain.order.OrderSide;
import com.moebius.entropy.domain.order.OrderStatus;
import com.moebius.entropy.domain.order.OrderType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BobooOpenOrderDto {
	@JsonProperty("clientOrderId")
	private String id;
	private String symbol;
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private BigDecimal price;
	@JsonProperty("origQty")
	private float originalQuantity;
	@JsonProperty("executedQty")
	private float executedQuantity;
	@JsonProperty("avgPrice")
	private float averagePrice;
	@JsonProperty("type")
	private OrderType orderType;
	@JsonProperty("side")
	private OrderSide orderSide;
	@JsonProperty("status")
	private OrderStatus orderStatus;
	@JsonProperty("orderId")
	private String internalId;
}
