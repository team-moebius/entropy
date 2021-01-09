package com.moebius.entropy.dto.order;

import com.moebius.entropy.domain.order.OrderSide;
import lombok.*;
import org.apache.commons.lang3.tuple.Pair;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DividedOrderDto {
	private OrderSide orderSide;
	private Pair<Integer, Integer> orderRange;
	private float period;
	private Pair<Integer, Integer> orderCountRange;
}
