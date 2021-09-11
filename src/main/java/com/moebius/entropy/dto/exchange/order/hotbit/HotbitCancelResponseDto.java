package com.moebius.entropy.dto.exchange.order.hotbit;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HotbitCancelResponseDto {
    String error;
    HotbitOrderDto result;
    String id;
}
