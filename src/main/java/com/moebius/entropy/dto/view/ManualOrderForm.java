package com.moebius.entropy.dto.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moebius.entropy.validators.ValidRangeField;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@ValidRangeField.List({
        @ValidRangeField(fieldRangeStart = "manualVolumeRangeFrom", fieldRangeEnd = "manualVolumeRangeTo"),
        @ValidRangeField(fieldRangeStart = "manualVolumeDivisionFrom", fieldRangeEnd = "manualVolumeDivisionTo"),
})
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManualOrderForm {
    @JsonProperty("order-position")
    @NotEmpty
    @Pattern(regexp = "^(SELL)|(BUY)$")
    private String orderPosition;
    @JsonProperty("manual-volume-range-from")
    @Positive
    private BigDecimal manualVolumeRangeFrom;
    @JsonProperty("manual-volume-range-to")
    @Positive
    private BigDecimal manualVolumeRangeTo;
    @JsonProperty("manual-volume-division-from")
    @Positive
    private long manualVolumeDivisionFrom;
    @JsonProperty("manual-volume-division-to")
    @Positive
    private long manualVolumeDivisionTo;
}
