package com.moebius.entropy.dto.exchange.order.bigone;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BigoneOrderRequestDto {
	@JsonProperty("asset_pair_name")
	private String symbol;
	private OrderPosition side;
	private BigDecimal price;
	private BigDecimal amount;
	private OrderType type;
	@JsonProperty("stop_price")
	private BigDecimal stopPrice;
	private String operator;
	@JsonProperty("immediate_or_cancel")
	private final boolean immediateOrCancel = false;
	@JsonProperty("post_only")
	private final boolean postOnly = true;
}
