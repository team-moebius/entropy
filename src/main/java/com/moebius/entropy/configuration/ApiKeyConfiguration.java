package com.moebius.entropy.configuration;

import com.moebius.entropy.domain.order.ApiKey;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "entropy")
public class ApiKeyConfiguration {
	private Map<String, ApiKey> apiKeys;
}
