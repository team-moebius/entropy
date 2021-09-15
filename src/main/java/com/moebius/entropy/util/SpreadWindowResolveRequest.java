package com.moebius.entropy.util;

import com.moebius.entropy.domain.trade.TradePrice;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.BinaryOperator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SpreadWindowResolveRequest {
    private int count;

    @Builder.Default
    private BigDecimal minimumVolume = new BigDecimal(Integer.MAX_VALUE);

    private BigDecimal startPrice;

    private BinaryOperator<BigDecimal> operationOnPrice;

    private int spreadWindow;

    private BigDecimal priceUnit;

    @Builder.Default
    private List<TradePrice> previousWindow = Collections.emptyList();

}
