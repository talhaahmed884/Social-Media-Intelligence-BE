package com.media.intelligence.social_account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Social account settings ({@code social-account.*} in application.yml).
 *
 * @param tokenEncryptionKey base64 AES key (16, 24 or 32 bytes) used to encrypt stored access tokens.
 *                           Generate with {@code openssl rand -base64 32}. Required to link accounts.
 * @param oauth              OAuth state settings
 * @param tokenRefresh       scheduled token refresh settings
 */
@ConfigurationProperties(prefix = "social-account")
public record SocialAccountProperties(
        String tokenEncryptionKey,
        @DefaultValue OAuth oauth,
        @DefaultValue TokenRefresh tokenRefresh
) {

    /**
     * @param stateSecret HMAC secret for signing OAuth state; random per start if blank
     *                    (fine for one instance, since states only live {@code stateTtl})
     * @param stateTtl    how long an authorization link stays valid
     */
    public record OAuth(
            String stateSecret,
            @DefaultValue("10m") Duration stateTtl
    ) {
        @Override
        public String toString() {
            return "OAuth[stateSecret=***, stateTtl=" + stateTtl + "]";
        }
    }

    /**
     * @param enabled       whether the scheduled refresh job runs
     * @param cron          when the job runs (UTC)
     * @param refreshBefore refresh tokens expiring within this window
     * @param minTokenAge   platforms reject refreshing very new tokens (Instagram: 24h)
     */
    public record TokenRefresh(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("0 0 3 * * *") String cron,
            @DefaultValue("10d") Duration refreshBefore,
            @DefaultValue("24h") Duration minTokenAge
    ) {
    }

    /**
     * Never print the encryption key.
     */
    @Override
    public String toString() {
        return "SocialAccountProperties[tokenEncryptionKey=***, oauth=" + oauth + ", tokenRefresh=" + tokenRefresh + "]";
    }
}
