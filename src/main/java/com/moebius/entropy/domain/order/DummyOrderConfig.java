package com.moebius.entropy.domain.order;

import lombok.*;
import org.apache.commons.lang3.tuple.Pair;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DummyOrderConfig {
	private Pair<Integer, Integer> orderRange;
	private float period;
	private Pair<Integer, Integer> orderCountRange;
}
