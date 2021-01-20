package com.moebius.entropy.domain.inflate;

import com.moebius.entropy.domain.Market;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class InflateRequest {

    private final Market market;
}
