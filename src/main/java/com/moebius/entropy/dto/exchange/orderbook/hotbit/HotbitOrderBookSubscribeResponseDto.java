package com.moebius.entropy.dto.exchange.orderbook.hotbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moebius.entropy.dto.exchange.orderbook.OrderBookDto;
import lombok.*;

import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotbitOrderBookSubscribeResponseDto {
    private String error;
    private Data result;
    private int id;

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String status;
    }
}
