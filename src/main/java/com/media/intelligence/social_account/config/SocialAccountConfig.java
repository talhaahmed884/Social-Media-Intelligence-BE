package com.media.intelligence.social_account.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables social account properties and scheduling (used by the token refresh job).
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(SocialAccountProperties.class)
public class SocialAccountConfig {
}
