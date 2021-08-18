package com.moebius.entropy.dto.exchange.order.bigone;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BigoneJwtPayloadDto {
	private String type;
	private String sub;
	private long nonce;
}
