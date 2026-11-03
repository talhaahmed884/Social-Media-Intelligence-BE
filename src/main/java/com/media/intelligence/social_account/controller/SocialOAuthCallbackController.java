package com.media.intelligence.social_account.controller;

import com.media.intelligence.common.dto.api_response.ApiResponse;
import com.media.intelligence.social_account.dto.SocialAccountResponseDTO;
import com.media.intelligence.social_account.service.SocialAccountService;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth redirect target. Configure its URL as the platform's redirect URI, e.g.
 * {@code social-connector.instagram.redirect-uri=https://<host>/api/v1/social-accounts/oauth/instagram/callback}.
 * <p>
 * The user is identified by the signed {@code state}, not by the path.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/social-accounts/oauth")
@RequiredArgsConstructor
public class SocialOAuthCallbackController {

    private final SocialAccountService socialAccountService;

    @GetMapping("/{platform}/callback")
    public ResponseEntity<ApiResponse<SocialAccountResponseDTO>> callback(
            @PathVariable String platform,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        // Never log the code or state
        log.info("GET /api/v1/social-accounts/oauth/{}/callback", platform);

        SocialAccountResponseDTO response = socialAccountService.completeAuthorization(
                SocialPlatform.fromValue(platform), code, state, error, errorDescription);
        return ResponseEntity.ok(ApiResponse.success("Social account linked successfully", response));
    }
}
