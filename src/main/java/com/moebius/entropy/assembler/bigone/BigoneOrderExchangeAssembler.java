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
	implements OrderExchangeAssembler<BigoneCancelRequestDto, BigoneOrderRequestDto, BigoneOrderResponseDto, BigoneOpenOrderDto> {

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
			.map(responseDto -> new Order(
				responseDto.getId(),
				SymbolUtil.marketFromSymbol(responseDto.getSymbol()),
				responseDto.getSide(),
				responseDto.getPrice(),
				OrderUtil.calculateRemainedVolume(responseDto.getAmount(), responseDto.getFilledAmount())
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
	public Order convertExchangeOrder(BigoneOpenOrderDto ordersFromExchange) {
		return Optional.ofNullable(ordersFromExchange)
			.map(dto -> new Order(
				dto.getId(),
				SymbolUtil.marketFromSymbol(dto.getSymbol()),
				dto.getSide(),
				dto.getPrice(),
				OrderUtil.calculateRemainedVolume(dto.getAmount(), dto.getFilledAmount())
			))
			.orElse(null);
	}
}
