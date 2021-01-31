package com.moebius.entropy.assembler;

import com.moebius.entropy.domain.ManualOrderMakingRequest;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.dto.view.ManualOrderForm;
import com.moebius.entropy.util.OrderUtil;
import org.springframework.stereotype.Component;

@Component
public class ManualOrderRequestAssembler {

    public ManualOrderMakingRequest assembleManualOrderRequest(Market market,
        ManualOrderForm manualOrderForm) {
        return ManualOrderMakingRequest.builder()
            .orderPosition(OrderUtil.resolveFromOrderSideString(manualOrderForm.getOrderPosition()))
            .startRange(Math.toIntExact(manualOrderForm.getManualVolumeDivisionFrom()))
            .endRange(Math.toIntExact(manualOrderForm.getManualVolumeDivisionTo()))
            .requestedVolumeFrom(manualOrderForm.getManualVolumeRangeFrom())
            .requestedVolumeTo(manualOrderForm.getManualVolumeRangeTo())
            .market(market)
            .build();
    }
}
