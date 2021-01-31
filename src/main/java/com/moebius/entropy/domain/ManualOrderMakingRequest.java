package com.moebius.entropy.domain;

import com.moebius.entropy.domain.order.OrderPosition;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ManualOrderMakingRequest {

    private OrderPosition orderPosition;
    private int startRange;
    private int endRange;
    private BigDecimal requestedVolumeFrom;
    private BigDecimal requestedVolumeTo;
    private Market market;
}
