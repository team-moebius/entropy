package com.moebius.entropy.domain.order;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiKey {
	private String accessKey;
	private String secretKey;
}
