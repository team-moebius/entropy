package com.moebius.entropy.dto.exchange.order.hotbit;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.*;

import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HotbitOpenOrderResponseDto {
    String error;
    Data result;
    String id;

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Data {
        InnerData data;

        @JsonAnySetter
        private void setAny(String key, InnerData innerData) {
            if (data != null) {
                throw new RuntimeException("try to bind twice");
            }
            data = innerData;
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class InnerData {
        int limit;
        int offset;
        int total;
        List<HotbitOrderDto> records;
    }
}
