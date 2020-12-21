package com.moebius.entropy.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

@Configuration
public class NetworkClientConfiguration {
	@Bean
	public WebClient webClient() {
		ClientHttpConnector connector = new ReactorClientHttpConnector();

		return WebClient.builder().clientConnector(connector).build();
	}

	@Bean
	public WebSocketClient webSocketClient() {
		return new ReactorNettyWebSocketClient();
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, false);
		return new ObjectMapper();
	}
}
