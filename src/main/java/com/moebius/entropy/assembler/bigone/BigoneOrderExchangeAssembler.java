package com.moebius.entropy.assembler.bigone;

import com.moebius.entropy.assembler.OrderExchangeAssembler;
import com.moebius.entropy.domain.order.Order;
import com.moebius.entropy.domain.order.OrderRequest;
import com.moebius.entropy.domain.order.OrderType;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneCancelRequestDto;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneOpenOrderDto;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneOrderRequestDto;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneOrderResponseDto;
import com.moebius.entropy.util.OrderUtil;
import com.moebius.entropy.util.SymbolUtil;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BigoneOrderExchangeAssembler
	implements OrderExchangeAssembler<BigoneCancelRequestDto, BigoneOrderRequestDto, BigoneOrderResponseDto, BigoneOpenOrderDto.Data> {

	@Override
	public BigoneOrderRequestDto convertToOrderRequest(OrderRequest orderRequest) {
		return Optional.ofNullable(orderRequest)
			.map(request -> BigoneOrderRequestDto.builder()
				.symbol(request.getMarket().getSymbol())
				.side(request.getOrderPosition())
				.price(request.getPrice())
				.amount(request.getVolume())
				.type(OrderType.LIMIT)
				.build())
			.orElse(null);
	}

	@Override
	public Order convertToOrder(BigoneOrderResponseDto orderResponse) {
		return Optional.ofNullable(orderResponse)
			.map(BigoneOrderResponseDto::getData)
			.map(data -> new Order(
				data.getId(),
				SymbolUtil.marketFromSymbol(data.getSymbol()),
				data.getSide(),
				data.getPrice(),
				OrderUtil.calculateRemainedVolume(data.getAmount(), data.getFilledAmount())
			))
			.orElse(null);
	}

	@Override
	public BigoneCancelRequestDto convertToCancelRequest(Order order) {
		return Optional.ofNullable(order)
			.map(cancelTarget -> BigoneCancelRequestDto.builder()
				.id(order.getOrderId())
				.build())
			.orElse(null);
	}

	@Override
	public Order convertExchangeOrder(BigoneOpenOrderDto.Data openOrderData) {
		return Optional.ofNullable(openOrderData)
			.map(data -> new Order(
				data.getId(),
				SymbolUtil.marketFromSymbol(data.getSymbol()),
				data.getSide(),
				data.getPrice(),
				OrderUtil.calculateRemainedVolume(data.getAmount(), data.getFilledAmount())
			))
			.orElse(null);
	}
}
