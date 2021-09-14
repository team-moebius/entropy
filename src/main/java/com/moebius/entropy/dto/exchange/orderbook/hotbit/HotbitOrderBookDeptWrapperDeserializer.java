package com.moebius.entropy.dto.exchange.orderbook.hotbit;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class HotbitOrderBookDeptWrapperDeserializer extends JsonDeserializer<HotbitOrderBookResponseDto.DepthWrapper> {
    @Override
    public HotbitOrderBookResponseDto.DepthWrapper deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        var codec = p.getCodec();
        JsonNode jsonNode = codec.readTree(p);
        var depth = codec.treeToValue(jsonNode.get(1), HotbitOrderBookResponseDto.Depth.class);
        return HotbitOrderBookResponseDto.DepthWrapper.builder()
                .resultStatus(jsonNode.get(0).asBoolean())
                .symbol(jsonNode.get(2).asText())
                .depth(depth)
                .build();

    }
}
