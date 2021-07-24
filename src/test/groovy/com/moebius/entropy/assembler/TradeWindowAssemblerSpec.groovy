package com.moebius.entropy.assembler

import com.moebius.entropy.domain.trade.TradeCurrency
import com.moebius.entropy.dto.exchange.orderbook.boboo.BobooOrderBookDto
import spock.lang.Specification

class TradeWindowAssemblerSpec extends Specification {
	TradeWindowAssembler sut = new TradeWindowAssembler()

	def "Test all data exist"() {
		given:
		def data = BobooOrderBookDto.builder()
				.symbol(bobooSymbol)
				.symbolName(bobooSymbol)
				.topic("depth")
				.params([:])
				.data([
						BobooOrderBookDto.Data.builder()
								.symbol(bobooSymbol)
								.timestamp("1565600357643")
								.version("112801745_18")
								.bids([["11371.49", "0.0014"],
									   ["11371.12", "0.2"],
									   ["11369.97", "0.3523"],
									   ["11369.96", "0.5"],
									   ["11369.95", "0.0934"],
									   ["11369.94", "1.6809"],
									   ["11369.6", "0.0047"],
									   ["11369.17", "0.3"],
									   ["11369.16", "0.2"],
									   ["11369.04", "1.3203"]])
								.asks([["11375.41", "0.0053"],
									   ["11375.42", "0.0043"],
									   ["11375.48", "0.0052"],
									   ["11375.58", "0.0541"],
									   ["11375.7", "0.0386"],
									   ["11375.71", "2"],
									   ["11377", "2.0691"],
									   ["11377.01", "0.0167"],
									   ["11377.12", "1.5"],
									   ["11377.61", "0.3"]])
								.build()
				])
				.build()
		when:
		def tradeWindow = sut.assembleTradeWindow(data)
		def market = sut.extractMarket(data)
		def marketPrice = sut.extractMarketPrice(data)

		then:
		def askPrices = tradeWindow.getAskPrices().collect { it.getUnitPrice() }
		askPrices == [11375.41, 11375.42, 11375.48, 11375.58, 11375.7, 11375.71, 11377, 11377.01, 11377.12, 11377.61]
		def askVolumes = tradeWindow.getAskPrices().collect { it.getVolume() }
		askVolumes == [0.0053, 0.0043, 0.0052, 0.0541, 0.0386, 2, 2.0691, 0.0167, 1.5, 0.3]

		def bidPrices = tradeWindow.getBidPrices().collect { it.getUnitPrice() }
		bidPrices == [11371.49, 11371.12, 11369.97, 11369.96, 11369.95, 11369.94, 11369.6, 11369.17, 11369.16, 11369.04]
		def bidVolumes = tradeWindow.getBidPrices().collect { it.getVolume() }
		bidVolumes == [0.0014, 0.2, 0.3523, 0.5, 0.0934, 1.6809, 0.0047, 0.3, 0.2, 1.3203]

		market.symbol == desiredSymbol
		market.tradeCurrency == desiredCurrency
		marketPrice == 11375.41

		where:
		bobooSymbol | desiredSymbol | desiredCurrency
		"GTAX2USDT" | "GTAX2USDT"   | TradeCurrency.USDT
		"MOIUSDT"   | "MOIUSDT"     | TradeCurrency.USDT
	}
}
