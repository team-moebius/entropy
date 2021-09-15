package com.moebius.entropy.assembler.hotbit;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.stream.Collectors;

public class HotbitSignature {

    public static String generate(
            String secretKey,
            MultiValueMap<String, String> queryParams
    ) {
        var queryString = UriComponentsBuilder.newInstance()
                .queryParams(queryParams)
                .build()
                .getQuery();

        var orderedQueryString = Arrays.stream(queryString.split("&")).sorted().collect(Collectors.joining("&"));
        var withSecretKey = String.format("%s&secret_key=%s", orderedQueryString, secretKey);

        return DigestUtils.md5Hex(withSecretKey).toUpperCase();
    }
}
