package com.moebius.entropy.dto.exchange.order.bigone;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BigoneCancelRequestDto {
	private String id;
}
