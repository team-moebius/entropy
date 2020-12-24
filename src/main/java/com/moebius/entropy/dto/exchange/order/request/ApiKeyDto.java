package com.moebius.entropy.dto.exchange.order.request;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class ApiKeyDto {
	private String accessKey;
	private String secretKey;
}
