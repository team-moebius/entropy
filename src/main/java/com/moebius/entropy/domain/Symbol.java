package com.moebius.entropy.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Symbol {
	GTAX2USDT("gtax2"),
	MOIUSDT("moi"),
	OAUSDT("oa");

	private final String key;
}
