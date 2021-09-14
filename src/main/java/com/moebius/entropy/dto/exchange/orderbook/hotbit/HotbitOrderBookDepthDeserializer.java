package com.moebius.entropy.dto.exchange.orderbook.hotbit;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class HotbitOrderBookDepthDeserializer extends JsonDeserializer<HotbitOrderBookResponseDto.Depth> {
    @Override
    public HotbitOrderBookResponseDto.Depth deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        var codec = p.getCodec();
        JsonNode jsonNode = codec.readTree(p);
        return HotbitOrderBookResponseDto.Depth.builder()
                .asks(deserializeData(codec, jsonNode.get("asks")))
                .bids(deserializeData(codec, jsonNode.get("bids")))
                .build();
    }

    private List<HotbitOrderBookResponseDto.Data> deserializeData(ObjectCodec codec, JsonNode jsonNode) throws JsonProcessingException {
        if (jsonNode == null) {
            return emptyList();
        }
        var list = (List<List<String>>) codec.treeToValue(jsonNode, List.class);
        return list.stream().map(o -> HotbitOrderBookResponseDto.Data.builder()
                        .price(new BigDecimal(o.get(0)))
                        .amount(new BigDecimal(o.get(1)))
                        .build())
                .collect(Collectors.toUnmodifiableList());
    }
}
