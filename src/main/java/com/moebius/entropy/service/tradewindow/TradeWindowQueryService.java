package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.TradeWindowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TradeWindowQueryService {

	private final TradeWindowRepository repository;

	public Mono<TradeWindow> fetchTradeWindow(Market market) {
		return Mono.justOrEmpty(repository.getTradeWindowForSymbol(market));
	}

	public BigDecimal getMarketPrice(Market market) {
		return repository.getMarketPriceForSymbol(market);
	}
}
