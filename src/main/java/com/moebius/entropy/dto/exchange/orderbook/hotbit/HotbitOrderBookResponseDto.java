package com.moebius.entropy.dto.exchange.orderbook.hotbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.moebius.entropy.dto.exchange.orderbook.OrderBookDto;
import lombok.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotbitOrderBookResponseDto implements OrderBookDto<HotbitOrderBookResponseDto.Depth> {
    private String method;
    private DepthWrapper params;
    private String id;

    @Override
    public String getSymbol() {
        return Optional.ofNullable(this.params)
                .map(DepthWrapper::getSymbol)
                .orElse(null);
    }

    @Override
    public List<Depth> getData() {
        return Optional.ofNullable(this.params)
                .map(DepthWrapper::getDepth)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @JsonDeserialize(using = HotbitOrderBookDeptWrapperDeserializer.class)
    public static class DepthWrapper {
        private boolean resultStatus;
        private String symbol;
        private Depth depth;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @JsonDeserialize(using = HotbitOrderBookDepthDeserializer.class)
    public static class Depth {
        private List<Data> bids;
        private List<Data> asks;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private BigDecimal price;
        private BigDecimal amount;
    }
}
