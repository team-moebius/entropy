package com.moebius.entropy.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class InflationConfig {

    private final int askCount;
    private final int bidCount;

}
