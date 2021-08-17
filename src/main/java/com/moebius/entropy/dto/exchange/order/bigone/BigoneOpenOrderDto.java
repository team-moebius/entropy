package com.moebius.entropy.dto.exchange.order.bigone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderStatus;
import com.moebius.entropy.domain.order.OrderType;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BigoneOpenOrderDto {
	private String id;
	@JsonProperty("asset_pair_name")
	private String symbol;
	private BigDecimal price;
	private BigDecimal amount;
	@JsonProperty("filled_amount")
	private BigDecimal filledAmount;
	@JsonProperty("avg_deal_price")
	private BigDecimal averageDealPrice;
	private OrderPosition side;
	private OrderStatus state;
	private OrderType type;
	@JsonProperty("stop_price")
	private BigDecimal stopPrice;
	private String operator;
	@JsonProperty("created_at")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime createdAt;
	@JsonProperty("updated_at")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime updatedAt;
}
