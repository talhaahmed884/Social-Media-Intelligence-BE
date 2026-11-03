package com.media.intelligence.social_account.service;

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
import com.media.intelligence.social_connector.dto.SocialCredentials;
import com.media.intelligence.social_connector.dto.SocialPost;
import com.media.intelligence.social_connector.dto.SocialToken;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.factory.SocialConnectorFactory;
import com.media.intelligence.social_connector.provider.SocialMediaProvider;
import com.media.intelligence.social_connector.provider.SocialOAuthProvider;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import com.media.intelligence.user.entity.User;
import com.media.intelligence.user.exception.UserErrorCode;
import com.media.intelligence.user.exception.UserException;
import com.media.intelligence.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.media.intelligence.social_account.SocialAccountTestFixtures.properties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialAccountService Tests")
public class SocialAccountServiceTest {

    private static final Instant NOW_INSTANT = Instant.parse("2026-01-01T00:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(NOW_INSTANT, ZoneOffset.UTC);
    private static final String TOKEN = "plain-token";
    private static final String ACCOUNT_ID = "17841400000000000";

    @Mock
    private SocialAccountRepository accountRepository;

    @Mock
    private UserService userService;

    @Mock
    private SocialConnectorFactory connectorFactory;

    @Mock
    private OAuthStateService stateService;

    @Mock
    private SocialMediaProvider connector;

    @Mock
    private SocialOAuthProvider oauthProvider;

    private final TokenCipher tokenCipher = new TokenCipher(properties());
    private SocialAccountService service;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        service = new SocialAccountService(
                accountRepository, userService, connectorFactory, tokenCipher, stateService,
                new LinkSocialAccountDTOValidator(), new LinkSocialAccountDTOSanitizer(),
                properties(), Clock.fixed(NOW_INSTANT, ZoneOffset.UTC));

        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("test@example.com").fullName("Test User").build();
    }

    // ==================== linkWithAccessToken Tests ====================

    @Test
    @DisplayName("linkWithAccessToken: Should verify token and save a new account with the token encrypted")
    void linkWithAccessToken_ShouldCreateAccount() {
        // Arrange
        stubLink();
        when(accountRepository.findByUser_IdAndPlatformAndPlatformAccountId(userId, SocialPlatform.INSTAGRAM, ACCOUNT_ID))
                .thenReturn(Optional.empty());

        // Act
        SocialAccountResponseDTO result = service.linkWithAccessToken(userId, SocialPlatform.INSTAGRAM,
                new LinkSocialAccountDTO("  " + TOKEN + "  "));

        // Assert
        ArgumentCaptor<SocialAuthRequest> authCaptor = ArgumentCaptor.forClass(SocialAuthRequest.class);
        verify(connector).connect(authCaptor.capture());
        assertEquals(TOKEN, authCaptor.getValue().getAccessToken(), "Token should be trimmed");

        SocialAccount saved = captureSaved();
        assertSame(user, saved.getUser());
        assertEquals(ACCOUNT_ID, saved.getPlatformAccountId());
        assertEquals("alice", saved.getUsername());
        assertEquals(SocialAccountStatus.ACTIVE, saved.getStatus());
        assertEquals(NOW, saved.getTokenIssuedAt());
        assertNull(saved.getTokenExpiresAt());
        assertNotEquals(TOKEN, saved.getAccessTokenEncrypted());
        assertEquals(TOKEN, tokenCipher.decrypt(saved.getAccessTokenEncrypted()));

        assertEquals(SocialPlatform.INSTAGRAM, result.platform());
        assertEquals("alice", result.username());
    }

    @Test
    @DisplayName("linkWithAccessToken: Should update and reactivate an already linked account")
    void linkWithAccessToken_ShouldUpdateExistingAccount() {
        // Arrange
        stubLink();
        SocialAccount existing = account(SocialAccountStatus.REAUTH_REQUIRED);
        when(accountRepository.findByUser_IdAndPlatformAndPlatformAccountId(userId, SocialPlatform.INSTAGRAM, ACCOUNT_ID))
                .thenReturn(Optional.of(existing));

        // Act
        service.linkWithAccessToken(userId, SocialPlatform.INSTAGRAM, new LinkSocialAccountDTO(TOKEN));

        // Assert
        SocialAccount saved = captureSaved();
        assertSame(existing, saved);
        assertEquals(SocialAccountStatus.ACTIVE, saved.getStatus());
        assertEquals(TOKEN, tokenCipher.decrypt(saved.getAccessTokenEncrypted()));
    }

    @Test
    @DisplayName("linkWithAccessToken: Should reject a blank token without calling the platform")
    void linkWithAccessToken_ShouldRejectBlankToken() {
        // Act
        SocialAccountException ex = assertThrows(SocialAccountException.class, () ->
                service.linkWithAccessToken(userId, SocialPlatform.INSTAGRAM, new LinkSocialAccountDTO("   ")));

        // Assert
        assertEquals(SocialAccountErrorCode.INVALID_INPUT, ex.getErrorCode());
        verifyNoInteractions(connectorFactory, accountRepository);
    }

    @Test
    @DisplayName("linkWithAccessToken: Should propagate USER_NOT_FOUND for unknown users")
    void linkWithAccessToken_ShouldFailForUnknownUser() {
        // Arrange
        when(userService.findUserById(userId)).thenThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        // Act
        UserException ex = assertThrows(UserException.class, () ->
                service.linkWithAccessToken(userId, SocialPlatform.INSTAGRAM, new LinkSocialAccountDTO(TOKEN)));

        // Assert
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(connectorFactory);
    }

    // ==================== OAuth Tests ====================

    @Test
    @DisplayName("startAuthorization: Should return the provider URL built with a signed state")
    void startAuthorization_ShouldReturnUrl() {
        // Arrange
        when(userService.findUserById(userId)).thenReturn(user);
        when(stateService.issue(userId, SocialPlatform.INSTAGRAM)).thenReturn("signed-state");
        when(connectorFactory.getOAuthProvider(SocialPlatform.INSTAGRAM)).thenReturn(oauthProvider);
        when(oauthProvider.buildAuthorizationUrl("signed-state")).thenReturn("https://auth.example?state=signed-state");

        // Act
        AuthorizationUrlResponseDTO result = service.startAuthorization(userId, SocialPlatform.INSTAGRAM);

        // Assert
        assertEquals("https://auth.example?state=signed-state", result.authorizationUrl());
    }

    @Test
    @DisplayName("completeAuthorization: Should exchange the code and link the account with token expiry")
    void completeAuthorization_ShouldLinkAccount() {
        // Arrange
        LocalDateTime expiresAt = NOW.plusDays(60);
        when(stateService.verify("signed-state", SocialPlatform.INSTAGRAM)).thenReturn(userId);
        when(connectorFactory.getOAuthProvider(SocialPlatform.INSTAGRAM)).thenReturn(oauthProvider);
        when(oauthProvider.exchangeAuthorizationCode("code-1")).thenReturn(new SocialToken(TOKEN, expiresAt));
        stubLink();
        when(accountRepository.findByUser_IdAndPlatformAndPlatformAccountId(any(), any(), any())).thenReturn(Optional.empty());

        // Act
        SocialAccountResponseDTO result = service.completeAuthorization(
                SocialPlatform.INSTAGRAM, "code-1", "signed-state", null, null);

        // Assert
        assertEquals(expiresAt, captureSaved().getTokenExpiresAt());
        assertEquals(expiresAt, result.tokenExpiresAt());
    }

    @Test
    @DisplayName("completeAuthorization: Should fail with OAUTH_AUTHORIZATION_DENIED when the user denied access")
    void completeAuthorization_ShouldHandleDenial() {
        // Act
        SocialAccountException ex = assertThrows(SocialAccountException.class, () -> service.completeAuthorization(
                SocialPlatform.INSTAGRAM, null, null, "access_denied", "The user denied your request"));

        // Assert
        assertEquals(SocialAccountErrorCode.OAUTH_AUTHORIZATION_DENIED, ex.getErrorCode());
        verifyNoInteractions(stateService, connectorFactory);
    }

    @Test
    @DisplayName("completeAuthorization: Should not exchange the code when the state is invalid")
    void completeAuthorization_ShouldRejectInvalidState() {
        // Arrange
        when(stateService.verify("forged", SocialPlatform.INSTAGRAM))
                .thenThrow(new SocialAccountException(SocialAccountErrorCode.INVALID_OAUTH_STATE));

        // Act
        SocialAccountException ex = assertThrows(SocialAccountException.class, () -> service.completeAuthorization(
                SocialPlatform.INSTAGRAM, "code-1", "forged", null, null));

        // Assert
        assertEquals(SocialAccountErrorCode.INVALID_OAUTH_STATE, ex.getErrorCode());
        verifyNoInteractions(connectorFactory);
    }

    // ==================== Fetch Tests ====================

    @Test
    @DisplayName("fetchPosts: Should call the connector with decrypted credentials for the account")
    void fetchPosts_ShouldUseDecryptedCredentials() {
        // Arrange
        SocialAccount account = account(SocialAccountStatus.ACTIVE);
        List<SocialPost> posts = List.of(SocialPost.builder().postId("m1").build());
        when(accountRepository.findByIdAndUser_Id(account.getId(), userId)).thenReturn(Optional.of(account));
        when(connectorFactory.getConnector(SocialPlatform.INSTAGRAM)).thenReturn(connector);
        when(connector.fetchPosts(any(), eq(ACCOUNT_ID), eq(10))).thenReturn(posts);

        // Act
        List<SocialPost> result = service.fetchPosts(userId, account.getId(), 10);

        // Assert
        ArgumentCaptor<SocialCredentials> captor = ArgumentCaptor.forClass(SocialCredentials.class);
        verify(connector).fetchPosts(captor.capture(), eq(ACCOUNT_ID), eq(10));
        assertEquals(new SocialCredentials(SocialPlatform.INSTAGRAM, ACCOUNT_ID, TOKEN, account.getTokenExpiresAt()),
                captor.getValue());
        assertSame(posts, result);
    }

    @Test
    @DisplayName("fetchPosts: Should mark REAUTH_REQUIRED without retrying when the stored token is unchanged")
    void fetchPosts_ShouldMarkReauthRequiredWhenTokenUnchanged() {
        // Arrange
        SocialAccount account = activeAccountWithFetchFailure();
        when(accountRepository.findTokenState(account.getId())).thenReturn(Optional.of(
                new SocialAccountTokenState(account.getAccessTokenEncrypted(), account.getTokenExpiresAt(), SocialAccountStatus.ACTIVE)));
        when(accountRepository.updateStatusIfTokenUnchanged(any(), any(), any(), any())).thenReturn(1);

        // Act
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                service.fetchPosts(userId, account.getId(), 10));

        // Assert
        assertEquals(SocialConnectorErrorCode.TOKEN_EXPIRED, ex.getErrorCode());
        verify(connector, times(1)).fetchPosts(any(), anyString(), anyInt());
        verify(accountRepository).updateStatusIfTokenUnchanged(
                eq(account.getId()), eq(account.getAccessTokenEncrypted()), eq(SocialAccountStatus.REAUTH_REQUIRED), any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("fetchPosts: Should retry once with the new token when it was refreshed during the request")
    void fetchPosts_ShouldRetryOnceWithRefreshedToken() {
        // Arrange
        SocialAccount account = account(SocialAccountStatus.ACTIVE);
        LocalDateTime newExpiry = NOW.plusDays(60);
        List<SocialPost> posts = List.of(SocialPost.builder().postId("m1").build());
        when(accountRepository.findByIdAndUser_Id(account.getId(), userId)).thenReturn(Optional.of(account));
        when(connectorFactory.getConnector(SocialPlatform.INSTAGRAM)).thenReturn(connector);
        when(connector.fetchPosts(any(), anyString(), anyInt()))
                .thenThrow(new SocialConnectorException(SocialConnectorErrorCode.TOKEN_EXPIRED))
                .thenReturn(posts);
        when(accountRepository.findTokenState(account.getId())).thenReturn(Optional.of(
                new SocialAccountTokenState(tokenCipher.encrypt("refreshed-token"), newExpiry, SocialAccountStatus.ACTIVE)));

        // Act
        List<SocialPost> result = service.fetchPosts(userId, account.getId(), 10);

        // Assert
        assertSame(posts, result);
        ArgumentCaptor<SocialCredentials> captor = ArgumentCaptor.forClass(SocialCredentials.class);
        verify(connector, times(2)).fetchPosts(captor.capture(), eq(ACCOUNT_ID), eq(10));
        assertEquals(TOKEN, captor.getAllValues().get(0).accessToken());
        assertEquals("refreshed-token", captor.getAllValues().get(1).accessToken());
        assertEquals(newExpiry, captor.getAllValues().get(1).expiresAt());
        verify(accountRepository, never()).updateStatusIfTokenUnchanged(any(), any(), any(), any());
    }

    @Test
    @DisplayName("fetchPosts: Should give up after the single retry and mark REAUTH_REQUIRED against the new token")
    void fetchPosts_ShouldFailAfterSingleRetry() {
        // Arrange
        SocialAccount account = activeAccountWithFetchFailure();
        String refreshedEncrypted = tokenCipher.encrypt("refreshed-token");
        when(accountRepository.findTokenState(account.getId())).thenReturn(Optional.of(
                new SocialAccountTokenState(refreshedEncrypted, NOW.plusDays(60), SocialAccountStatus.ACTIVE)));
        when(accountRepository.updateStatusIfTokenUnchanged(any(), any(), any(), any())).thenReturn(1);

        // Act
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                service.fetchPosts(userId, account.getId(), 10));

        // Assert
        assertEquals(SocialConnectorErrorCode.TOKEN_EXPIRED, ex.getErrorCode());
        verify(connector, times(2)).fetchPosts(any(), anyString(), anyInt());
        verify(accountRepository).updateStatusIfTokenUnchanged(
                eq(account.getId()), eq(refreshedEncrypted), eq(SocialAccountStatus.REAUTH_REQUIRED), any());
    }

    @Test
    @DisplayName("fetchPosts: Should not retry when the account was already flagged, unlinked, or re-encrypted with the same token")
    void fetchPosts_ShouldNotRetryWithoutANewActiveToken() {
        // Arrange
        SocialAccount account = activeAccountWithFetchFailure();
        when(accountRepository.findTokenState(account.getId()))
                .thenReturn(Optional.of(new SocialAccountTokenState(
                        tokenCipher.encrypt("refreshed-token"), null, SocialAccountStatus.REAUTH_REQUIRED)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new SocialAccountTokenState(
                        tokenCipher.encrypt(TOKEN), null, SocialAccountStatus.ACTIVE)));

        // Act & Assert: three requests, one call each
        for (int i = 0; i < 3; i++) {
            assertThrows(SocialConnectorException.class, () -> service.fetchPosts(userId, account.getId(), 10));
        }
        verify(connector, times(3)).fetchPosts(any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("fetchPosts: Should not reload or retry on non-token errors")
    void fetchPosts_ShouldNotRetryOnOtherErrors() {
        // Arrange
        SocialAccount account = account(SocialAccountStatus.ACTIVE);
        when(accountRepository.findByIdAndUser_Id(account.getId(), userId)).thenReturn(Optional.of(account));
        when(connectorFactory.getConnector(SocialPlatform.INSTAGRAM)).thenReturn(connector);
        when(connector.fetchPosts(any(), anyString(), anyInt()))
                .thenThrow(new SocialConnectorException(SocialConnectorErrorCode.PLATFORM_UNAVAILABLE));

        // Act
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                service.fetchPosts(userId, account.getId(), 10));

        // Assert
        assertEquals(SocialConnectorErrorCode.PLATFORM_UNAVAILABLE, ex.getErrorCode());
        verify(accountRepository, never()).findTokenState(any());
        verify(connector, times(1)).fetchPosts(any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("fetchPosts: Should fail fast for accounts that need re-authorization")
    void fetchPosts_ShouldFailForReauthRequiredAccount() {
        // Arrange
        SocialAccount account = account(SocialAccountStatus.REAUTH_REQUIRED);
        when(accountRepository.findByIdAndUser_Id(account.getId(), userId)).thenReturn(Optional.of(account));

        // Act
        SocialAccountException ex = assertThrows(SocialAccountException.class, () ->
                service.fetchPosts(userId, account.getId(), 10));

        // Assert
        assertEquals(SocialAccountErrorCode.REAUTHORIZATION_REQUIRED, ex.getErrorCode());
        verifyNoInteractions(connectorFactory);
    }

    @Test
    @DisplayName("fetchPosts: Should return SOCIAL_ACCOUNT_NOT_FOUND for accounts of other users")
    void fetchPosts_ShouldNotExposeOtherUsersAccounts() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdAndUser_Id(accountId, userId)).thenReturn(Optional.empty());

        // Act
        SocialAccountException ex = assertThrows(SocialAccountException.class, () ->
                service.fetchPosts(userId, accountId, 10));

        // Assert
        assertEquals(SocialAccountErrorCode.SOCIAL_ACCOUNT_NOT_FOUND, ex.getErrorCode());
    }

    // ==================== Token Refresh Tests ====================

    @Test
    @DisplayName("refreshExpiringTokens: Should refresh due tokens, flag rejected ones, and keep transient failures")
    void refreshExpiringTokens_ShouldHandleEachOutcome() {
        // Arrange
        SocialAccount refreshable = account(SocialAccountStatus.ACTIVE, "token-a");
        SocialAccount revoked = account(SocialAccountStatus.ACTIVE, "token-b");
        SocialAccount unavailable = account(SocialAccountStatus.ACTIVE, "token-c");

        when(accountRepository.findTokensDueForRefresh(
                SocialAccountStatus.ACTIVE, NOW.minusHours(24), NOW.plusDays(10)))
                .thenReturn(List.of(refreshable, revoked, unavailable));
        when(connectorFactory.getOAuthProvider(SocialPlatform.INSTAGRAM)).thenReturn(oauthProvider);
        when(oauthProvider.refreshToken("token-a")).thenReturn(new SocialToken("token-a2", NOW.plusDays(60)));
        when(oauthProvider.refreshToken("token-b"))
                .thenThrow(new SocialConnectorException(SocialConnectorErrorCode.AUTHENTICATION_FAILED));
        when(oauthProvider.refreshToken("token-c"))
                .thenThrow(new SocialConnectorException(SocialConnectorErrorCode.PLATFORM_UNAVAILABLE));
        when(accountRepository.updateTokenIfUnchanged(any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(accountRepository.updateStatusIfTokenUnchanged(any(), any(), any(), any())).thenReturn(1);

        // Act
        int refreshed = service.refreshExpiringTokens();

        // Assert
        assertEquals(1, refreshed);

        ArgumentCaptor<String> newToken = ArgumentCaptor.forClass(String.class);
        verify(accountRepository).updateTokenIfUnchanged(eq(refreshable.getId()), eq(refreshable.getAccessTokenEncrypted()),
                newToken.capture(), eq(NOW.plusDays(60)), eq(NOW), any());
        assertEquals("token-a2", tokenCipher.decrypt(newToken.getValue()));

        verify(accountRepository).updateStatusIfTokenUnchanged(
                eq(revoked.getId()), eq(revoked.getAccessTokenEncrypted()), eq(SocialAccountStatus.REAUTH_REQUIRED), any());
        verify(accountRepository, never()).updateStatusIfTokenUnchanged(eq(unavailable.getId()), any(), any(), any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("refreshExpiringTokens: Should keep a token that was re-linked while refreshing")
    void refreshExpiringTokens_ShouldNotOverwriteNewerToken() {
        // Arrange
        SocialAccount account = account(SocialAccountStatus.ACTIVE, "token-a");
        when(accountRepository.findTokensDueForRefresh(any(), any(), any())).thenReturn(List.of(account));
        when(connectorFactory.getOAuthProvider(SocialPlatform.INSTAGRAM)).thenReturn(oauthProvider);
        when(oauthProvider.refreshToken("token-a")).thenReturn(new SocialToken("token-a2", NOW.plusDays(60)));
        when(accountRepository.updateTokenIfUnchanged(any(), any(), any(), any(), any(), any())).thenReturn(0);

        // Act & Assert
        assertEquals(0, service.refreshExpiringTokens());
    }

    // ==================== Unlink Tests ====================

    @Test
    @DisplayName("unlinkAccount: Should delete the user's account")
    void unlinkAccount_ShouldDelete() {
        // Arrange
        SocialAccount account = account(SocialAccountStatus.ACTIVE);
        when(accountRepository.findByIdAndUser_Id(account.getId(), userId)).thenReturn(Optional.of(account));

        // Act
        service.unlinkAccount(userId, account.getId());

        // Assert
        verify(accountRepository).delete(account);
    }

    // ==================== Helpers ====================

    private void stubLink() {
        when(userService.findUserById(userId)).thenReturn(user);
        when(connectorFactory.getConnector(SocialPlatform.INSTAGRAM)).thenReturn(connector);
        when(connector.connect(any())).thenReturn(SocialAuthResponse.builder()
                .authenticated(true)
                .platform(SocialPlatform.INSTAGRAM)
                .userId(ACCOUNT_ID)
                .username("alice")
                .build());
        when(accountRepository.save(any(SocialAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * An ACTIVE account whose fetches always fail with TOKEN_EXPIRED.
     */
    private SocialAccount activeAccountWithFetchFailure() {
        SocialAccount account = account(SocialAccountStatus.ACTIVE);
        when(accountRepository.findByIdAndUser_Id(account.getId(), userId)).thenReturn(Optional.of(account));
        when(connectorFactory.getConnector(SocialPlatform.INSTAGRAM)).thenReturn(connector);
        when(connector.fetchPosts(any(), anyString(), anyInt()))
                .thenThrow(new SocialConnectorException(SocialConnectorErrorCode.TOKEN_EXPIRED));
        return account;
    }

    private SocialAccount captureSaved() {
        ArgumentCaptor<SocialAccount> captor = ArgumentCaptor.forClass(SocialAccount.class);
        verify(accountRepository).save(captor.capture());
        return captor.getValue();
    }

    private SocialAccount account(SocialAccountStatus status) {
        return account(status, TOKEN);
    }

    private SocialAccount account(SocialAccountStatus status, String token) {
        return SocialAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .platform(SocialPlatform.INSTAGRAM)
                .platformAccountId(ACCOUNT_ID)
                .username("alice")
                .accessTokenEncrypted(tokenCipher.encrypt(token))
                .tokenExpiresAt(NOW.plusDays(5))
                .tokenIssuedAt(NOW.minusDays(55))
                .status(status)
                .build();
    }
}
