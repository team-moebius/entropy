package com.moebius.entropy.util;

import java.util.UUID;

public class OrderIdUtil {
    public static String generateOrderId(){
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}
