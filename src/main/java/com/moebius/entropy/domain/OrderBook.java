package com.moebius.entropy.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@ToString
public class OrderBook {
	private String symbol;
	private List<String> bids;
	private List<String> asks;
	private LocalDateTime createdAt;
}
