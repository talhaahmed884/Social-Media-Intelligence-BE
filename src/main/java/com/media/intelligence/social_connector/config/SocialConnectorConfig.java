package com.media.intelligence.social_connector.config;

import com.media.intelligence.social_connector.resilience.RetryPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Clock;

/**
 * Shared infrastructure beans for social connectors.
 * Platform-specific beans (e.g. each platform's RestClient) live in the platform's own config class.
 */
@Configuration
@EnableConfigurationProperties(SocialConnectorProperties.class)
public class SocialConnectorConfig {

    /**
     * Retry policy used by connectors for transient platform errors.
     */
    @Bean
    public RetryPolicy socialConnectorRetryPolicy(SocialConnectorProperties properties) {
        SocialConnectorProperties.Retry retry = properties.retry();
        return RetryPolicy.builder()
                .maxAttempts(retry.maxAttempts())
                .initialDelayMs(retry.initialDelay().toMillis())
                .backoffMultiplier(retry.backoffMultiplier())
                .maxDelayMs(retry.maxDelay().toMillis())
                .build();
    }

    /**
     * UTC clock for token expiry and OAuth state timestamps (replace in tests).
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Build a request factory with the configured timeouts for a platform RestClient.
     * Each platform config calls this so all connectors share the same HTTP settings.
     */
    public static ClientHttpRequestFactory requestFactory(SocialConnectorProperties.Http http) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(http.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(http.readTimeout());
        return factory;
    }
}
