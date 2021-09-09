package com.moebius.entropy.dto.exchange.order.hotbit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
public class HotbitOpenOrderRequestDto {
    /**
     * market name，for example："BTC/USDT","ETH/USDT"
     */
    @JsonProperty("market")
    String symbol;

    @Builder.Default
    int offset = 0;

    @Builder.Default
    int limit = 200;


    @With
    @JsonProperty("api_key")
    String accessKey;
}
