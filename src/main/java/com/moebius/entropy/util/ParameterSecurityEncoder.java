package com.moebius.entropy.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Streams;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StreamUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ParameterSecurityEncoder {
    private static final String SIGNATURE_HASH_ALGORITHM = "HmacSHA256";
    private static final String QUERY_PARAMETER_FORMAT = "%s=%s";
    private static final String QUERY_PARAMETER_SEPARATOR = "&";

    public static String encodeParameters(Map<String, String> queryParams,
                                          Map<String, String> bodyValues,
                                          String baseSecretKey) {
        byte[] keyBytes = baseSecretKey.getBytes();
        final SecretKeySpec secretKey = new SecretKeySpec(keyBytes, SIGNATURE_HASH_ALGORITHM);

        String rawQueryString = toQueryString(queryParams);
        String rawBodyValue = toQueryString(bodyValues);
        String targetForMAC = rawQueryString + rawBodyValue;
        log.info(targetForMAC);
        byte[] bytesToBeSigned = targetForMAC.getBytes();

        try {
            Mac mac = Mac.getInstance(SIGNATURE_HASH_ALGORITHM);
            mac.init(secretKey);
            byte[] bytes = mac.doFinal(bytesToBeSigned);
            return bytesToHexString(bytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Hashing parameters failed for query param: {}, bodyValue: {}, due to", rawQueryString, rawBodyValue, e);
            return null;
        }
    }

    private static String toQueryString(Map<String, String> param) {
        return param.entrySet().stream()
                .filter(entry -> StringUtils.isNoneEmpty(entry.getKey(), entry.getValue()))
                .map(entry -> String.format(QUERY_PARAMETER_FORMAT, entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(QUERY_PARAMETER_SEPARATOR));
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder hexStringBuffer = new StringBuilder();
        for (byte hashedByte : bytes){
            char firstChar = Character.forDigit((hashedByte >> 4) & 0xF, 16);
            char secondChar = Character.forDigit((hashedByte & 0xF), 16);
            hexStringBuffer.append(firstChar);
            hexStringBuffer.append(secondChar);
        }
        return hexStringBuffer.toString();
    }

}
