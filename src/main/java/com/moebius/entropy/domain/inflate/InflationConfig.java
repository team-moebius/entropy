package com.moebius.entropy.domain.inflate;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
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
