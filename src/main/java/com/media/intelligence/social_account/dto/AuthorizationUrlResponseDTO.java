package com.media.intelligence.social_account.dto;

import com.media.intelligence.social_connector.provider.SocialPlatform;

/**
 * URL to send the user to for granting access. The flow finishes at the OAuth callback endpoint.
 */
public record AuthorizationUrlResponseDTO(SocialPlatform platform, String authorizationUrl) {
}
