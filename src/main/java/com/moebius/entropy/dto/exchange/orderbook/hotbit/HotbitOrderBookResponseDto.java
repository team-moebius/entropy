package com.moebius.entropy.dto.exchange.orderbook.hotbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moebius.entropy.dto.exchange.orderbook.OrderBookDto;
import lombok.*;

import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotbitOrderBookResponseDto implements OrderBookDto<HotbitOrderBookResponseDto.Data> {
    private String error;
    private Data result;
    private int id;
    @With
    private String symbol;

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public List<Data> getData() {
        return null;
    }

    /**
     * {
     *     "error": null,
     *     "result": { "asks": [[ "8000", "20"] ], "bids": [[ "800", "4"] ] },
     *     "id": 100
     * }
     */

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private List<List<String>> bids;
        private List<List<String>> asks;
    }
}
