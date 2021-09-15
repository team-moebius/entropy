package com.moebius.entropy.assembler.hotbit;

import com.moebius.entropy.assembler.TradeWindowAssembler;
import com.moebius.entropy.domain.Exchange;
import com.moebius.entropy.domain.order.OrderPosition;
import com.moebius.entropy.domain.trade.TradePrice;
import com.moebius.entropy.dto.exchange.orderbook.OrderBookDto;
import com.moebius.entropy.dto.exchange.orderbook.hotbit.HotbitOrderBookResponseDto;
import com.moebius.entropy.service.tradewindow.TradeWindowQueryService;
import com.moebius.entropy.util.SymbolUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HotbitTradeWindowAssembler extends TradeWindowAssembler<HotbitOrderBookResponseDto.DepthWrapper> {
    private final TradeWindowQueryService tradeWindowQueryService;

    @Override
    public BigDecimal extractMarketPrice(OrderBookDto<HotbitOrderBookResponseDto.DepthWrapper> orderBookDto) {
        return Optional.ofNullable(orderBookDto)
                .map(OrderBookDto::getData)
                .map(this::findFirst)
                .map(this::getMarketPrice)
                .orElseThrow(() -> new IllegalStateException(
                        String.format(
                                "[%s] Failed to extract market price from BigoneOrderBookDto due to data missing %s",
                                getClass().getName(), orderBookDto
                        )
                ));
    }

    private BigDecimal getMarketPrice(HotbitOrderBookResponseDto.DepthWrapper depthWrapper) {
        var market = SymbolUtil.marketFromSymbol(depthWrapper.getSymbol());
        var tradeWindow = tradeWindowQueryService.getTradeWindow(market);
        var depth = depthWrapper.getDepth();

        if (CollectionUtils.isEmpty(tradeWindow.getAskPrices()) && CollectionUtils.isEmpty(tradeWindow.getBidPrices())) {
            if (CollectionUtils.isEmpty(depth.getAsks())) {
                return Optional.ofNullable(depth.getBids())
                        .map(this::findFirst)
                        .map(HotbitOrderBookResponseDto.Data::getPrice)
                        .map(highestBidPrice -> highestBidPrice.add(market.getTradeCurrency().getPriceUnit()))
                        .orElse(null);
            } else {
                return Optional.ofNullable(depth.getAsks())
                        .map(this::findFirst)
                        .map(HotbitOrderBookResponseDto.Data::getPrice)
                        .orElse(null);
            }
        }

        return tradeWindow.getAskPrices().stream()
                .map(TradePrice::getUnitPrice)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Exchange getExchange() {
        return Exchange.HOTBIT;
    }

    @Override
    protected List<TradePrice> mapTrade(OrderPosition orderPosition, HotbitOrderBookResponseDto.DepthWrapper depthWrapper) {
        return Optional.ofNullable(getOrderPositionData(orderPosition, depthWrapper.getDepth()))
                .stream()
                .flatMap(Collection::stream)
                .map(data -> new TradePrice(orderPosition, data.getPrice(), data.getAmount()))
                .collect(Collectors.toUnmodifiableList());
    }

    List<HotbitOrderBookResponseDto.Data> getOrderPositionData(OrderPosition orderPosition, HotbitOrderBookResponseDto.Depth data) {
        if (orderPosition == OrderPosition.ASK) {
            return data.getAsks();
        }
        return data.getBids();
    }
}
