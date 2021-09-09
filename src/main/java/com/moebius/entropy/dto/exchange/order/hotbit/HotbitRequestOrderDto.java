package com.moebius.entropy.dto.exchange.order.hotbit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
public class HotbitRequestOrderDto {
    /**
     * User's API KEY
     */
    @With
    @JsonProperty("api_key")
    String accessKey;

    /**
     * market name，for example："BTC/USDT","ETH/USDT"
     */
    @JsonProperty("market")
    String symbol;
    /**
     * 1 = "sell"，2="buy"
     */
    int side;
    double amount;
    double price;
    /**
     * apply the discount of discount tokens or not 0 = "No(no)"，1="Yes(yes)"
     */
    @JsonProperty("isfee")
    int isFee;
}
