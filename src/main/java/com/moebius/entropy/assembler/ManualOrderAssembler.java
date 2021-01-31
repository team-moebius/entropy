package com.moebius.entropy.assembler;

import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.ManualOrderMakingRequest;
import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.order.OrderSide;
import com.moebius.entropy.domain.trade.TradeCurrency;
import com.moebius.entropy.dto.order.ManualOrderRequestDto;
import com.moebius.entropy.util.OrderUtil;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ManualOrderAssembler {
    public ManualOrderMakingRequest assembleFromRequestDto(ManualOrderRequestDto requestDto) {
        return Optional.of(requestDto)
                .map(dto -> ManualOrderMakingRequest.builder()
                    .orderPosition(
                        OrderUtil.resolveFromOrderSide(OrderSide.valueOf(dto.getOrderSide())))
                    .startRange(dto.getStartRange())
                    .endRange(dto.getEndRange())
                    .requestedVolumeFrom(dto.getRequestedVolumeFrom())
                    .requestedVolumeTo(dto.getRequestedVolumeTo())
                        .market(new Market(
                                Exchange.valueOf(dto.getExchange()), dto.getSymbol(), TradeCurrency.valueOf(dto.getTradeCurrency())
                        ))
                        .build())
                .orElseThrow();
    }
}
