package com.moebius.entropy.dto.order;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ManualOrderRequestDto {
    private String orderSide;
    private int startRange;
    private int endRange;
    private BigDecimal requestedVolume;
    private String exchange;
    private String symbol;
    private String tradeCurrency;
}
