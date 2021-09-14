package com.moebius.entropy.service.inflate.bigone;

import org.springframework.web.reactive.socket.WebSocketMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class ZipUtils {
    public static String decompress(WebSocketMessage message) throws IOException {
        var is = message.getPayload().asInputStream();
        try (var gis = new GZIPInputStream(is)) {
            return new String(gis.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw e;
        }
    }
}
