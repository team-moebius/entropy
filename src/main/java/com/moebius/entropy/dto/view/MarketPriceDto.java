package com.moebius.entropy.dto.view;

import com.moebius.entropy.domain.Exchange;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MarketPriceDto {

    private Exchange exchange;
    private String symbol;
    private String tradeCurrency;
    private BigDecimal priceUnit;
    private BigDecimal price;
}
