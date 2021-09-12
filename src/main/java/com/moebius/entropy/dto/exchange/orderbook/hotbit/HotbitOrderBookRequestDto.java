package com.moebius.entropy.dto.exchange.orderbook.hotbit;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Getter
public class HotbitOrderBookRequestDto {
    String method = "depth.subscribe";
    /**
     * [ "BTCBCC", # Market Name 100, # Available price level of order：1, 5, 10, 20, 30, 50, 100 "0.0001" # Precision on price range, available values："0","0.00000001","0.0000001","0.000001","0.00001", "0.0001", "0.001", "0.01", "0.1" ]
     */
    List<Object> params;
    String id;

    /**
     *
     * @param symbol marketName
     * @param priceLevel # available price level of order：1, 5, 10, 20, 30, 50, 100
     * @param pricePrecision Precision，for example"0.001"，the data retrieved will be the type of numbers such as：12.975. Another example"5"，you will retrieve numbers such as "15" "20" "10" .
     */
    @Builder
    public HotbitOrderBookRequestDto(String symbol, int priceLevel, double pricePrecision) {
        params = new ArrayList<>();
        params.add(symbol);
        params.add(priceLevel);
        params.add(pricePrecision);
        id = "id"; // TODO:
    }
}
