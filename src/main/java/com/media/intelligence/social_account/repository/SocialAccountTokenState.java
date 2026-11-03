package com.media.intelligence.social_account.repository;

import com.media.intelligence.social_account.entity.SocialAccountStatus;

import java.time.LocalDateTime;

/**
 * Current token columns of a social account, read straight from the database.
 * <p>
 * Being a DTO projection (not an entity), it bypasses the persistence context, so it sees
 * changes committed by other transactions (e.g. the refresh job) during an open-in-view request.
 *
 * @param accessTokenEncrypted encrypted token
 * @param tokenExpiresAt       token expiry (UTC), or null if unknown
 * @param status               account status
 */
public record SocialAccountTokenState(
        String accessTokenEncrypted,
        LocalDateTime tokenExpiresAt,
        SocialAccountStatus status
) {
    @Override
    public String toString() {
        return "SocialAccountTokenState[accessTokenEncrypted=***, tokenExpiresAt=" + tokenExpiresAt + ", status=" + status + "]";
    }
}
