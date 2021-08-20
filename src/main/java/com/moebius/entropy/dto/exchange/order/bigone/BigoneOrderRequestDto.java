package com.moebius.entropy.dto.exchange.order.bigone;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.order.OrderType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BigoneOrderRequestDto {
	@JsonProperty("asset_pair_name")
	private String symbol;
	private OrderPosition side;
	private BigDecimal price;
	private BigDecimal amount;
	private OrderType type;
}
