package com.moebius.entropy.domain.order;

import lombok.*;

@Getter
@Setter
@ToString
public class ApiKey {
	private String accessKey;
	private String secretKey;
}
