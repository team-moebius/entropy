package com.moebius.entropy.domain.order;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.util.List;

@Getter
@Builder
@ToString
public class DummyOrderRequest {
	private List<OrderRequest> orderRequests;
	private Duration delay;
	private int reorderCount;
}
