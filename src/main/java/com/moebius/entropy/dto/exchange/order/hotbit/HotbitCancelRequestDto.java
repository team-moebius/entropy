package com.moebius.entropy.dto.exchange.order.hotbit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Value
@Builder
public class HotbitCancelRequestDto {
    /**
     * market name，for example："BTC/USDT","ETH/USDT"
     */
    @JsonProperty("market")
    String symbol;
    /**
     * id that requires the cancellation of transaction
     */
    @JsonProperty("order_id")
    int orderId;

    @With
    @JsonProperty("api_key")
    String accessKey;
}
