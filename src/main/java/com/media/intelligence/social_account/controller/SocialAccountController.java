package com.media.intelligence.social_account.controller;

import com.media.intelligence.common.dto.api_response.ApiResponse;
import com.media.intelligence.social_account.dto.AuthorizationUrlResponseDTO;
import com.media.intelligence.social_account.dto.LinkSocialAccountDTO;
import com.media.intelligence.social_account.dto.SocialAccountResponseDTO;
import com.media.intelligence.social_account.service.SocialAccountService;
import com.media.intelligence.social_connector.dto.SocialComment;
import com.media.intelligence.social_connector.dto.SocialPost;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for a user's linked social accounts and their data.
 * <p>
 * Platform path values are case-insensitive (e.g. {@code instagram}).
 * Limits above the platform maximum are rejected with SC6002.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/social-accounts")
@RequiredArgsConstructor
public class SocialAccountController {

    private static final String DEFAULT_LIMIT = "25";

    private final SocialAccountService socialAccountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SocialAccountResponseDTO>>> listAccounts(@PathVariable UUID userId) {
        log.info("GET /api/v1/users/{}/social-accounts", userId);
        return ResponseEntity.ok(ApiResponse.success(socialAccountService.listAccounts(userId)));
    }

    /**
     * Start OAuth: returns the URL to open; the platform redirects to the OAuth callback endpoint.
     */
    @PostMapping("/{platform}/authorize")
    public ResponseEntity<ApiResponse<AuthorizationUrlResponseDTO>> startAuthorization(
            @PathVariable UUID userId,
            @PathVariable String platform) {

        log.info("POST /api/v1/users/{}/social-accounts/{}/authorize", userId, platform);

        AuthorizationUrlResponseDTO response =
                socialAccountService.startAuthorization(userId, SocialPlatform.fromValue(platform));
        return ResponseEntity.ok(ApiResponse.success("Open the authorization URL to link the account", response));
    }

    /**
     * Link with an existing access token (no OAuth redirect).
     */
    @PostMapping("/{platform}/token")
    public ResponseEntity<ApiResponse<SocialAccountResponseDTO>> linkWithAccessToken(
            @PathVariable UUID userId,
            @PathVariable String platform,
            @RequestBody LinkSocialAccountDTO dto) {

        log.info("POST /api/v1/users/{}/social-accounts/{}/token", userId, platform);

        SocialAccountResponseDTO response =
                socialAccountService.linkWithAccessToken(userId, SocialPlatform.fromValue(platform), dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Social account linked successfully", response));
    }

    @DeleteMapping("/{socialAccountId}")
    public ResponseEntity<ApiResponse<Void>> unlinkAccount(
            @PathVariable UUID userId,
            @PathVariable UUID socialAccountId) {

        log.info("DELETE /api/v1/users/{}/social-accounts/{}", userId, socialAccountId);

        socialAccountService.unlinkAccount(userId, socialAccountId);
        return ResponseEntity.ok(ApiResponse.success("Social account unlinked successfully", null));
    }

    @GetMapping("/{socialAccountId}/posts")
    public ResponseEntity<ApiResponse<List<SocialPost>>> fetchPosts(
            @PathVariable UUID userId,
            @PathVariable UUID socialAccountId,
            @RequestParam(defaultValue = DEFAULT_LIMIT) int limit) {

        log.info("GET /api/v1/users/{}/social-accounts/{}/posts?limit={}", userId, socialAccountId, limit);
        return ResponseEntity.ok(ApiResponse.success(socialAccountService.fetchPosts(userId, socialAccountId, limit)));
    }

    @GetMapping("/{socialAccountId}/posts/{postId}")
    public ResponseEntity<ApiResponse<SocialPost>> fetchPost(
            @PathVariable UUID userId,
            @PathVariable UUID socialAccountId,
            @PathVariable String postId) {

        log.info("GET /api/v1/users/{}/social-accounts/{}/posts/{}", userId, socialAccountId, postId);
        return ResponseEntity.ok(ApiResponse.success(socialAccountService.fetchPost(userId, socialAccountId, postId)));
    }

    @GetMapping("/{socialAccountId}/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<SocialComment>>> fetchComments(
            @PathVariable UUID userId,
            @PathVariable UUID socialAccountId,
            @PathVariable String postId,
            @RequestParam(defaultValue = DEFAULT_LIMIT) int limit) {

        log.info("GET /api/v1/users/{}/social-accounts/{}/posts/{}/comments?limit={}", userId, socialAccountId, postId, limit);
        return ResponseEntity.ok(ApiResponse.success(
                socialAccountService.fetchComments(userId, socialAccountId, postId, limit)));
    }
}
