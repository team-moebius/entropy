package com.moebius.entropy.controller;

import com.moebius.entropy.assembler.ManualOrderAssembler;
import com.moebius.entropy.domain.ManualOrderResult;
import com.moebius.entropy.dto.order.ManualOrderRequestDto;
import com.moebius.entropy.service.trade.manual.ManualOrderMakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@RequestMapping("/manual-trade")
@RequiredArgsConstructor
@Slf4j
public class ManualTradeController {
    private final ManualOrderMakerService orderMakerService;
    private final ManualOrderAssembler assembler;

    @PostMapping
    public Mono<ManualOrderResult> requestManualOrder(@RequestBody ManualOrderRequestDto request) {
        return Optional.of(request)
                .map(assembler::assembleFromRequestDto)
                .map(orderMakerService::requestManualOrderMaking)
                .orElseThrow();
    }
}
