package com.moebius.entropy.dto.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moebius.entropy.validators.ValidRangeField;
import lombok.*;

import javax.validation.constraints.Positive;
import java.math.BigDecimal;


@ValidRangeField.List({
        @ValidRangeField(fieldRangeStart = "sellVolumeRangeFrom", fieldRangeEnd = "sellVolumeRangeTo"),
        @ValidRangeField(fieldRangeStart = "sellDivisionFrom", fieldRangeEnd = "sellDivisionTo"),
        @ValidRangeField(fieldRangeStart = "sellDivisionTimeFrom", fieldRangeEnd = "sellDivisionTimeTo"),
        @ValidRangeField(fieldRangeStart = "autoSellMarketPriceVolumeFrom", fieldRangeEnd = "autoSellMarketPriceVolumeTo"),
        @ValidRangeField(fieldRangeStart = "autoSellMarketPriceTimeFrom", fieldRangeEnd = "autoSellMarketPriceTimeTo"),
        @ValidRangeField(fieldRangeStart = "buyVolumeRangeFrom", fieldRangeEnd = "buyVolumeRangeTo"),
        @ValidRangeField(fieldRangeStart = "buyDivisionFrom", fieldRangeEnd = "buyDivisionTo"),
        @ValidRangeField(fieldRangeStart = "buyDivisionTimeFrom", fieldRangeEnd = "buyDivisionTimeTo"),
        @ValidRangeField(fieldRangeStart = "autoBuyMarketPriceVolumeFrom", fieldRangeEnd = "autoBuyMarketPriceVolumeTo"),
        @ValidRangeField(fieldRangeStart = "autoBuyMarketPriceTimeFrom", fieldRangeEnd = "autoBuyMarketPriceTimeTo")
})
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutomaticOrderForm {
    @JsonProperty("sell-inflation-count")
    @Positive
    private long sellInflationCount;

    @JsonProperty("sell-volume-range-from")
    @Positive
    private BigDecimal sellVolumeRangeFrom;

    @JsonProperty("sell-volume-range-to")
    @Positive
    private BigDecimal sellVolumeRangeTo;

    @JsonProperty("sell-division-from")
    @Positive
    private long sellDivisionFrom;

    @JsonProperty("sell-division-to")
    @Positive
    private long sellDivisionTo;

    @JsonProperty("sell-division-interval")
    @Positive
    private BigDecimal sellDivisionInterval;

    @JsonProperty("sell-division-time-from")
    @Positive
    private long sellDivisionTimeFrom;

    @JsonProperty("sell-division-time-to")
    @Positive
    private long sellDivisionTimeTo;

    @JsonProperty("auto-sell-market-price-volume-from")
    @Positive
    private BigDecimal autoSellMarketPriceVolumeFrom;

    @JsonProperty("auto-sell-market-price-volume-to")
    @Positive
    private BigDecimal autoSellMarketPriceVolumeTo;

    @JsonProperty("auto-sell-market-price-interval")
    @Positive
    private BigDecimal autoSellMarketPriceInterval;

    @JsonProperty("auto-sell-market-price-time-from")
    @Positive
    private long autoSellMarketPriceTimeFrom;

    @JsonProperty("auto-sell-market-price-time-to")
    @Positive
    private long autoSellMarketPriceTimeTo;

    @JsonProperty("buy-inflation-count")
    @Positive
    private long buyInflationCount;

    @JsonProperty("buy-volume-range-from")
    @Positive
    private BigDecimal buyVolumeRangeFrom;

    @JsonProperty("buy-volume-range-to")
    @Positive
    private BigDecimal buyVolumeRangeTo;

    @JsonProperty("buy-division-from")
    @Positive
    private long buyDivisionFrom;

    @JsonProperty("buy-division-to")
    @Positive
    private long buyDivisionTo;

    @JsonProperty("buy-division-interval")
    @Positive
    private BigDecimal buyDivisionInterval;

    @JsonProperty("buy-division-time-from")
    @Positive
    private long buyDivisionTimeFrom;

    @JsonProperty("buy-division-time-to")
    @Positive
    private long buyDivisionTimeTo;

    @JsonProperty("auto-buy-market-price-volume-from")
    @Positive
    private BigDecimal autoBuyMarketPriceVolumeFrom;

    @JsonProperty("auto-buy-market-price-volume-to")
    @Positive
    private BigDecimal autoBuyMarketPriceVolumeTo;

    @JsonProperty("auto-buy-market-price-interval")
    @Positive
    private BigDecimal autoBuyMarketPriceInterval;

    @JsonProperty("auto-buy-market-price-time-from")
    @Positive
    private long autoBuyMarketPriceTimeFrom;

    @JsonProperty("auto-buy-market-price-time-to")
    @Positive
    private long autoBuyMarketPriceTimeTo;
}
