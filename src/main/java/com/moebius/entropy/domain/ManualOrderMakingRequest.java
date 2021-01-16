package com.moebius.entropy.domain;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ManualOrderMakingRequest {
    private OrderPosition orderPosition;
    private int startRange;
    private int endRange;
    private BigDecimal requestedVolume;
    private Market market;
}
