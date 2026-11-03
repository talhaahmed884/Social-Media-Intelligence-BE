package com.media.intelligence.social_account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that keeps long-lived tokens from expiring (Instagram: 60 days, refreshable after 24h).
 * Disable with {@code social-account.token-refresh.enabled=false} (the test profile does).
 * <p>
 * Runs on every instance; with several instances, add a lock (e.g. ShedLock) before scaling out.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "social-account.token-refresh", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SocialTokenRefreshJob {

    private final SocialAccountService socialAccountService;

    @Scheduled(cron = "${social-account.token-refresh.cron:0 0 3 * * *}", zone = "UTC")
    public void refreshTokens() {
        log.info("Running scheduled social token refresh");
        socialAccountService.refreshExpiringTokens();
    }
}
