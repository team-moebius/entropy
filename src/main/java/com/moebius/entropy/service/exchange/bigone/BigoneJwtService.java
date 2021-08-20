package com.moebius.entropy.service.exchange.bigone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moebius.entropy.domain.order.ApiKey;
import com.moebius.entropy.dto.exchange.order.bigone.BigoneJwtPayloadDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BigoneJwtService {
	private final static String ALGORITHM_KEY = "alg";
	private final static String HEADER_TYPE_KEY = "typ";
	private final static int NANOS_CORRECTION_FACTOR = 1000000;

	private final ObjectMapper objectMapper;

	@Value("${exchange.bigone.auth.header.alg}")
	private String algorithm;
	@Value("${exchange.bigone.auth.header.typ}")
	private String headerType;
	@Value("${exchange.bigone.auth.payload.type}")
	private String payloadType;

	public String create(ApiKey apiKey) {
		Map<String, Object> header = new HashMap<>();
		header.put(ALGORITHM_KEY, algorithm);
		header.put(HEADER_TYPE_KEY, headerType);

		try {
			return Jwts.builder()
				.setHeader(header)
				.setPayload(objectMapper.writeValueAsString(BigoneJwtPayloadDto.builder()
					.type(payloadType)
					.sub(apiKey.getAccessKey())
					.nonce(String.valueOf(System.currentTimeMillis() * NANOS_CORRECTION_FACTOR))
					.build()))
				.signWith(Keys.hmacShaKeyFor(apiKey.getSecretKey().getBytes()), SignatureAlgorithm.HS256)
				.compact();
		} catch (JsonProcessingException e) {
			log.warn("[Bigone] Failed to processing json.", e);
			return null;
		}

	}
}
