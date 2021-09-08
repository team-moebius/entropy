package com.moebius.entropy.configuration.hotbit;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class HotbitConfiguration {
    private final WebSocket webSocket = new WebSocket();
    private final Rest rest = new Rest();

    @Getter
    public static class WebSocket {
        private String uri = "wss://ws.hotbit.io/v2/";
        private long timeout = 60000;
    }

    @Getter
    public static class Rest {
        private String scheme = "https";
        private String host = "api.hotbit.io";
        private String openOrders = "/v2/p2/order.pending";
        private String cancelOrders = "/v2/p2/order.cancel";
        private String requestOrders = "/v2/p2/order.put_limit";
        private String orderBook = "/v2/p1/order.book";
    }
}
