package com.moebius.entropy.dto.exchange.order;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiKeyDto {
	private String accessKey;
	private String secretKey;
}
