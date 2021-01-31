package com.moebius.entropy.dto.order;


import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ManualOrderRequestDto {

    private String orderSide;
    private int startRange;
    private int endRange;
    private BigDecimal requestedVolumeFrom;
    private BigDecimal requestedVolumeTo;
    private String exchange;
    private String symbol;
    private String tradeCurrency;
}
