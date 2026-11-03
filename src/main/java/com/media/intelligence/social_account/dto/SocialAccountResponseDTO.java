package com.media.intelligence.social_account.dto;

import com.media.intelligence.social_account.entity.SocialAccount;
import com.media.intelligence.social_account.entity.SocialAccountStatus;
import com.media.intelligence.social_connector.provider.SocialPlatform;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Linked social account as returned by the API. Never contains the access token.
 */
public record SocialAccountResponseDTO(
        UUID id,
        SocialPlatform platform,
        String platformAccountId,
        String username,
        SocialAccountStatus status,
        LocalDateTime tokenExpiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SocialAccountResponseDTO fromEntity(SocialAccount account) {
        return new SocialAccountResponseDTO(
                account.getId(),
                account.getPlatform(),
                account.getPlatformAccountId(),
                account.getUsername(),
                account.getStatus(),
                account.getTokenExpiresAt(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
