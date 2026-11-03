package com.media.intelligence.social_connector.connector.instagram;

import com.media.intelligence.social_connector.config.SocialConnectorConfig;
import com.media.intelligence.social_connector.config.SocialConnectorProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Beans for the Instagram connector.
 */
@Configuration
@EnableConfigurationProperties(InstagramProperties.class)
public class InstagramConfig {

    public static final String REST_CLIENT = "instagramRestClient";

    /**
     * RestClient used by {@link InstagramApiClient}. Full URLs are built per request because
     * data, token-exchange and code-exchange endpoints live on different hosts/paths.
     */
    @Bean(REST_CLIENT)
    public RestClient instagramRestClient(RestClient.Builder builder, SocialConnectorProperties properties) {
        return builder
                .requestFactory(SocialConnectorConfig.requestFactory(properties.http()))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
