package com.moebius.entropy.dto.view;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AutomaticOrderCancelResult {

    private List<String> cancelledDisposableIds;
    private boolean inflationCancelled;
}
