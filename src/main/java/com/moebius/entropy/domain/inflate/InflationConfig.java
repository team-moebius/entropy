package com.moebius.entropy.domain.inflate;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Builder.Default
    private final boolean enable = false;

    public InflationConfig disable() {
        return InflationConfig.builder()
            .askCount(askCount)
            .bidCount(bidCount)
            .askMinVolume(askMinVolume)
            .askMaxVolume(askMaxVolume)
            .bidMinVolume(bidMinVolume)
            .bidMaxVolume(bidMaxVolume)
            .enable(false)
            .build();
    }
}
