package com.moebius.entropy.assembler;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.inflate.InflationConfig;
import com.moebius.entropy.dto.order.DividedDummyOrderDto;
import com.moebius.entropy.dto.view.AutomaticOrderForm;
import com.moebius.entropy.dto.view.AutomaticOrderResult;
import org.springframework.stereotype.Component;

@Component
public class AutomaticOrderViewAssembler {

    public InflationConfig assembleInflationConfig(AutomaticOrderForm automaticOrderForm) {
        return null;
    }

    public DividedDummyOrderDto assembleDivideDummyOrder(Market market,
        AutomaticOrderForm automaticOrderForm) {
        return null;
    }

    public AutomaticOrderResult assembleAutomaticOrderResult(String disposableId) {
        return null;
    }
}
