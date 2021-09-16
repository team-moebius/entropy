package com.moebius.entropy.dto.util;

import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
@Getter
@EqualsAndHashCode
public class PriceAndVolume {

    private final BigDecimal price;
    private final BigDecimal volume;
}
