package com.moebius.entropy.dto.exchange.order.hotbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotbitOrderDto {
    /**
     * order-Id
     */
    private int id;
    /**
     * Transaction Pair
     */
    private String market;
    /**
     * Identifier on source of data request
     */
    private String source;
    /**
     * Order placement type 1-limit order
     */
    private String type;
    /**
     * Identifier of buyer and seller 1-Seller，2-Buyer
     */
    private String side;
    /**
     * Identifier of buyer and seller 1-Seller，2-Buyer
     */
    private int user;
    /**
     * #Time of order creation(second)
     */
    @JsonProperty("ctime")
    private double createTime;

    /**
     * #Time of order update(second)
     */
    @JsonProperty("mtime")
    private double updateTime;

    private double price;

    private String amount;

    @JsonProperty("taker_fee")
    private String takerFee;

    @JsonProperty("maker_fee")
    private String makerFee;

    private String left;

    @JsonProperty("deal_stock")
    private String dealStock;

    @JsonProperty("deal_money")
    private String dealMoney;

    @JsonProperty("deal_fee")
    private String dealFee;

    /**
     * #Identifier of order status When true with 0x8, it means the current order is cancelled, when true with 0x80, it means the current order enjoys discount.
     */
    private String status;

    /**
     * #Name of Discount Token
     */
    @JsonProperty("fee_stock")
    private String fee_stock;

    /**
     * #Discount Applied to the Discount Token
     */
    @JsonProperty("alt_fee")
    private String altFee;

    /**
     * #Volume Deducted as Discount
     */
    @JsonProperty("deal_fee_alt")
    private String dealFeeAlt;
}
