package com.moebius.entropy.service.tradewindow;

import com.moebius.entropy.domain.Market;
import com.moebius.entropy.domain.trade.TradeWindow;
import com.moebius.entropy.repository.TradeDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TradeWindowQueryService {

	private final TradeDataRepository repository;

	public Mono<TradeWindow> getTradeWindowMono(Market market) {
		return Mono.justOrEmpty(repository.getTradeWindowByMarket(market));
	}

	public TradeWindow getTradeWindow(Market market) {
		return repository.getTradeWindowByMarket(market);
	}

	public BigDecimal getMarketPrice(Market market) {
		return repository.getMarketPriceByMarket(market);
	}
}
