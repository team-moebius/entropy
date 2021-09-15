package com.moebius.entropy.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "exchange.hotbit")
public class HotbitProperties {
    WebSocket webSocket;
    Rest rest;

    @Getter
    @Setter
    public static class WebSocket {
        String uri;
        long timeout;
    }

    @Getter
    @Setter
    public static class Rest {
        String scheme;
        String host;
        String openOrders;
        String cancelOrders;
        String requestOrders;
        String orderBook;
    }
}
