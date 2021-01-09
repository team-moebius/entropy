package com.moebius.entropy.domain;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InflationConfig {

    private int askCount;
    private int bidCount;
    private BigDecimal askMinVolume;
    private BigDecimal askMaxVolume;
    private BigDecimal bidMinVolume;
    private BigDecimal bidMaxVolume;

}
