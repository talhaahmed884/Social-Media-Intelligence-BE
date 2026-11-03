package com.media.intelligence.social_account.entity;

/**
 * Lifecycle state of a linked social account.
 */
public enum SocialAccountStatus {
    /**
     * Token is usable.
     */
    ACTIVE,

    /**
     * Platform rejected the token (revoked/expired). The user must link the account again.
     */
    REAUTH_REQUIRED
}
