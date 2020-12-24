package com.moebius.entropy.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class InflateRequest {

    private final Market market;
}
