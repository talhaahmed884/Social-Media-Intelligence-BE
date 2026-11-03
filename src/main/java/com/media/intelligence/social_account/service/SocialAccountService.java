package com.media.intelligence.social_account.service;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.social_account.config.SocialAccountProperties;
import com.media.intelligence.social_account.crypto.TokenCipher;
import com.media.intelligence.social_account.dto.AuthorizationUrlResponseDTO;
import com.media.intelligence.social_account.dto.LinkSocialAccountDTO;
import com.media.intelligence.social_account.dto.SocialAccountResponseDTO;
import com.media.intelligence.social_account.entity.SocialAccount;
import com.media.intelligence.social_account.entity.SocialAccountStatus;
import com.media.intelligence.social_account.exception.SocialAccountErrorCode;
import com.media.intelligence.social_account.exception.SocialAccountException;
import com.media.intelligence.social_account.oauth.OAuthStateService;
import com.media.intelligence.social_account.repository.SocialAccountRepository;
import com.media.intelligence.social_account.repository.SocialAccountTokenState;
import com.media.intelligence.social_account.sanitization.LinkSocialAccountDTOSanitizer;
import com.media.intelligence.social_account.validation.LinkSocialAccountDTOValidator;
import com.media.intelligence.social_connector.dto.SocialAuthRequest;
import com.media.intelligence.social_connector.dto.SocialAuthResponse;
import com.media.intelligence.social_connector.dto.SocialComment;
import com.media.intelligence.social_connector.dto.SocialCredentials;
import com.media.intelligence.social_connector.dto.SocialPost;
import com.media.intelligence.social_connector.dto.SocialToken;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.factory.SocialConnectorFactory;
import com.media.intelligence.social_connector.provider.SocialMediaProvider;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import com.media.intelligence.user.entity.User;
import com.media.intelligence.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Links social accounts to users, stores their tokens encrypted, keeps tokens fresh, and
 * proxies data fetches through the connector layer with the right credentials.
 * <p>
 * External HTTP calls are made outside database transactions; only the final saves touch the DB.
 * <p>
 * When a platform rejects a token (AUTHENTICATION_FAILED / TOKEN_EXPIRED), the service re-reads the stored
 * token once: if it changed while the request was in flight (refresh job or re-link), the call is retried
 * a single time with the new token. Otherwise the account is marked REAUTH_REQUIRED and further fetches
 * fail fast with SACC_007 until the user links it again.
 * <p>
 * Status and token writes that can race (fetch vs. refresh job vs. re-link) are conditional on the stored
 * token being unchanged, so a stale copy never overwrites a newer token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAccountService {

    private static final Set<SocialConnectorErrorCode> REAUTH_ERRORS = EnumSet.of(
            SocialConnectorErrorCode.AUTHENTICATION_FAILED,
            SocialConnectorErrorCode.TOKEN_EXPIRED
    );

    private final SocialAccountRepository accountRepository;
    private final UserService userService;
    private final SocialConnectorFactory connectorFactory;
    private final TokenCipher tokenCipher;
    private final OAuthStateService stateService;
    private final LinkSocialAccountDTOValidator linkValidator;
    private final LinkSocialAccountDTOSanitizer linkSanitizer;
    private final SocialAccountProperties properties;
    private final Clock clock;

    // ==================== LINKING ====================

    /**
     * Start the OAuth flow: returns the URL the user should open to grant access.
     *
     * @throws com.media.intelligence.user.exception.UserException if the user does not exist
     * @throws SocialConnectorException                            if the platform has no (configured) OAuth support
     */
    public AuthorizationUrlResponseDTO startAuthorization(UUID userId, SocialPlatform platform) {
        userService.findUserById(userId);
        requireEncryptionConfigured();

        String state = stateService.issue(userId, platform);
        String url = connectorFactory.getOAuthProvider(platform).buildAuthorizationUrl(state);

        log.info("Started {} authorization for user {}", platform, userId);
        return new AuthorizationUrlResponseDTO(platform, url);
    }

    /**
     * Finish the OAuth flow from the platform redirect: verify state, exchange the code, link the account.
     *
     * @param platform         platform of the callback endpoint
     * @param code             authorization code (absent if denied)
     * @param state            state issued by {@link #startAuthorization}
     * @param error            error from the platform (e.g. access_denied), if any
     * @param errorDescription platform's error description, if any
     */
    public SocialAccountResponseDTO completeAuthorization(SocialPlatform platform, String code, String state,
                                                          String error, String errorDescription) {
        if (error != null) {
            log.warn("{} authorization denied: {} ({})", platform, error, errorDescription);
            throw new SocialAccountException(SocialAccountErrorCode.OAUTH_AUTHORIZATION_DENIED);
        }

        UUID userId = stateService.verify(state, platform);

        if (code == null || code.isBlank()) {
            throw new SocialAccountException(SocialAccountErrorCode.INVALID_INPUT);
        }

        SocialToken token = connectorFactory.getOAuthProvider(platform).exchangeAuthorizationCode(code);
        return link(userId, platform, token);
    }

    /**
     * Link an account with an existing access token (no OAuth redirect).
     * The token's expiry is unknown until the first scheduled refresh.
     */
    public SocialAccountResponseDTO linkWithAccessToken(UUID userId, SocialPlatform platform, LinkSocialAccountDTO dto) {
        if (dto == null) {
            throw new SocialAccountException(SocialAccountErrorCode.INVALID_INPUT);
        }

        linkSanitizer.sanitize(dto);
        ValidationResult validationResult = linkValidator.validate(dto);
        if (validationResult.hasErrors()) {
            log.warn("Link social account validation failed: {}", validationResult.getDetailedErrorMessage());
            throw new SocialAccountException(SocialAccountErrorCode.INVALID_INPUT);
        }

        return link(userId, platform, new SocialToken(dto.getAccessToken(), null));
    }

    private SocialAccountResponseDTO link(UUID userId, SocialPlatform platform, SocialToken token) {
        User user = userService.findUserById(userId);
        requireEncryptionConfigured();

        // Verify the token and learn which platform account it belongs to
        SocialAuthResponse auth = connectorFactory.getConnector(platform).connect(SocialAuthRequest.builder()
                .platform(platform)
                .accessToken(token.accessToken())
                .build());

        SocialAccount account = accountRepository
                .findByUser_IdAndPlatformAndPlatformAccountId(userId, platform, auth.getUserId())
                .orElseGet(() -> SocialAccount.builder()
                        .user(user)
                        .platform(platform)
                        .platformAccountId(auth.getUserId())
                        .build());

        account.setUsername(auth.getUsername());
        account.setStatus(SocialAccountStatus.ACTIVE);
        applyToken(account, token);

        SocialAccount saved = accountRepository.save(account);
        log.info("Linked {} account {} ({}) to user {}", platform, auth.getUserId(), auth.getUsername(), userId);
        return SocialAccountResponseDTO.fromEntity(saved);
    }

    // ==================== ACCOUNT MANAGEMENT ====================

    @Transactional(readOnly = true)
    public List<SocialAccountResponseDTO> listAccounts(UUID userId) {
        userService.findUserById(userId);
        return accountRepository.findByUser_IdOrderByCreatedAtAsc(userId).stream()
                .map(SocialAccountResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void unlinkAccount(UUID userId, UUID socialAccountId) {
        SocialAccount account = getOwnedAccount(userId, socialAccountId);
        accountRepository.delete(account);
        log.info("Unlinked {} account {} from user {}", account.getPlatform(), account.getPlatformAccountId(), userId);
    }

    // ==================== DATA FETCHING ====================

    /**
     * Fetch the linked account's own posts.
     */
    public List<SocialPost> fetchPosts(UUID userId, UUID socialAccountId, int limit) {
        SocialAccount account = getActiveAccount(userId, socialAccountId);
        return withConnector(account, (connector, credentials) ->
                connector.fetchPosts(credentials, account.getPlatformAccountId(), limit));
    }

    public SocialPost fetchPost(UUID userId, UUID socialAccountId, String postId) {
        SocialAccount account = getActiveAccount(userId, socialAccountId);
        return withConnector(account, (connector, credentials) -> connector.fetchPost(credentials, postId));
    }

    public List<SocialComment> fetchComments(UUID userId, UUID socialAccountId, String postId, int limit) {
        SocialAccount account = getActiveAccount(userId, socialAccountId);
        return withConnector(account, (connector, credentials) -> connector.fetchComments(credentials, postId, limit));
    }

    // ==================== TOKEN REFRESH ====================

    /**
     * Refresh tokens that expire within {@code social-account.token-refresh.refresh-before}
     * (or whose expiry is unknown) and are old enough to be refreshed.
     *
     * @return number of tokens refreshed
     */
    public int refreshExpiringTokens() {
        LocalDateTime now = now();
        SocialAccountProperties.TokenRefresh settings = properties.tokenRefresh();

        List<SocialAccount> due = accountRepository.findTokensDueForRefresh(
                SocialAccountStatus.ACTIVE,
                now.minus(settings.minTokenAge()),
                now.plus(settings.refreshBefore()));

        int refreshed = 0;
        for (SocialAccount account : due) {
            if (refreshToken(account)) {
                refreshed++;
            }
        }

        log.info("Token refresh finished: {} of {} due accounts refreshed", refreshed, due.size());
        return refreshed;
    }

    private boolean refreshToken(SocialAccount account) {
        String usedToken = account.getAccessTokenEncrypted();
        try {
            SocialToken token = connectorFactory.getOAuthProvider(account.getPlatform())
                    .refreshToken(tokenCipher.decrypt(usedToken));

            // Conditional write: if the user re-linked meanwhile, keep their newer token
            int updated = accountRepository.updateTokenIfUnchanged(
                    account.getId(), usedToken, tokenCipher.encrypt(token.accessToken()),
                    token.expiresAt(), now(), auditNow());
            if (updated == 0) {
                log.info("Token for account {} changed during refresh; keeping the newer token", account.getId());
                return false;
            }
            return true;

        } catch (SocialConnectorException e) {
            if (REAUTH_ERRORS.contains(e.getErrorCode())) {
                markReauthRequired(account.getId(), usedToken, e);
            } else if (e.getErrorCode() == SocialConnectorErrorCode.UNSUPPORTED_PLATFORM) {
                log.debug("{} has no token refresh support; skipping account {}", account.getPlatform(), account.getId());
            } else {
                log.warn("Token refresh failed for account {} (will retry next run): {}", account.getId(), e.getMessage());
            }
        } catch (SocialAccountException e) {
            log.error("Token refresh failed for account {}: {}", account.getId(), e.getMessage());
        }
        return false;
    }

    // ==================== HELPERS ====================

    /**
     * Run a connector call with the account's token. On a token rejection, retry at most once,
     * and only if the stored token changed while the call was in flight.
     */
    private <T> T withConnector(SocialAccount account, BiFunction<SocialMediaProvider, SocialCredentials, T> call) {
        SocialMediaProvider connector = connectorFactory.getConnector(account.getPlatform());
        String usedToken = account.getAccessTokenEncrypted();

        try {
            return call.apply(connector, credentials(account, usedToken, account.getTokenExpiresAt()));
        } catch (SocialConnectorException e) {
            if (!REAUTH_ERRORS.contains(e.getErrorCode())) {
                throw e;
            }

            SocialAccountTokenState latest = accountRepository.findTokenState(account.getId()).orElse(null);
            if (!tokenChanged(usedToken, latest)) {
                markReauthRequired(account.getId(), usedToken, e);
                throw e;
            }

            log.info("Token for {} account {} changed during the request; retrying once with the latest token",
                    account.getPlatform(), account.getId());
            try {
                return call.apply(connector,
                        credentials(account, latest.accessTokenEncrypted(), latest.tokenExpiresAt()));
            } catch (SocialConnectorException retryError) {
                if (REAUTH_ERRORS.contains(retryError.getErrorCode())) {
                    markReauthRequired(account.getId(), latest.accessTokenEncrypted(), retryError);
                }
                throw retryError;
            }
        }
    }

    /**
     * True if the account still exists, is ACTIVE, and now holds a different token than the one used.
     * Compares decrypted values because every encryption uses a fresh IV.
     */
    private boolean tokenChanged(String usedToken, SocialAccountTokenState latest) {
        return latest != null
                && latest.status() == SocialAccountStatus.ACTIVE
                && !tokenCipher.decrypt(latest.accessTokenEncrypted()).equals(tokenCipher.decrypt(usedToken));
    }

    private SocialCredentials credentials(SocialAccount account, String encryptedToken, LocalDateTime expiresAt) {
        return new SocialCredentials(
                account.getPlatform(),
                account.getPlatformAccountId(),
                tokenCipher.decrypt(encryptedToken),
                expiresAt);
    }

    private void applyToken(SocialAccount account, SocialToken token) {
        account.setAccessTokenEncrypted(tokenCipher.encrypt(token.accessToken()));
        account.setTokenExpiresAt(token.expiresAt());
        account.setTokenIssuedAt(now());
    }

    /**
     * Mark REAUTH_REQUIRED only if the stored token is still the rejected one; a token refreshed or
     * re-linked in the meantime keeps the account ACTIVE.
     */
    private void markReauthRequired(UUID accountId, String rejectedToken, SocialConnectorException cause) {
        int updated = accountRepository.updateStatusIfTokenUnchanged(
                accountId, rejectedToken, SocialAccountStatus.REAUTH_REQUIRED, auditNow());
        if (updated > 0) {
            log.warn("Social account {} needs re-authorization: {}", accountId, cause.getErrorCode());
        } else {
            log.info("Social account {} token changed meanwhile; not marking re-authorization", accountId);
        }
    }

    private SocialAccount getActiveAccount(UUID userId, UUID socialAccountId) {
        SocialAccount account = getOwnedAccount(userId, socialAccountId);
        if (account.getStatus() == SocialAccountStatus.REAUTH_REQUIRED) {
            throw new SocialAccountException(SocialAccountErrorCode.REAUTHORIZATION_REQUIRED);
        }
        return account;
    }

    private SocialAccount getOwnedAccount(UUID userId, UUID socialAccountId) {
        if (userId == null || socialAccountId == null) {
            throw new SocialAccountException(SocialAccountErrorCode.INVALID_INPUT);
        }
        return accountRepository.findByIdAndUser_Id(socialAccountId, userId)
                .orElseThrow(() -> new SocialAccountException(SocialAccountErrorCode.SOCIAL_ACCOUNT_NOT_FOUND));
    }

    private void requireEncryptionConfigured() {
        if (!tokenCipher.isConfigured()) {
            throw new SocialAccountException(SocialAccountErrorCode.TOKEN_ENCRYPTION_NOT_CONFIGURED);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * Value for updated_at in bulk updates, which skip @UpdateTimestamp. Uses the JVM zone like
     * Hibernate does for the audit columns (token columns stay UTC).
     */
    private LocalDateTime auditNow() {
        return LocalDateTime.now(clock.withZone(ZoneId.systemDefault()));
    }
}
