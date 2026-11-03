package com.media.intelligence.social_account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.intelligence.social_account.crypto.TokenCipher;
import com.media.intelligence.social_account.dto.LinkSocialAccountDTO;
import com.media.intelligence.social_account.entity.SocialAccount;
import com.media.intelligence.social_account.repository.SocialAccountRepository;
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
import com.media.intelligence.user.repository.UserRepository;
import com.media.intelligence.user_credential.repository.UserCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack tests (HTTP → service → H2) with the platform side mocked at the connector factory.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SocialAccountController Integration Tests")
public class SocialAccountControllerIntegrationTest {

    private static final String TOKEN = "plain-instagram-token";
    private static final String ACCOUNT_ID = "17841400000000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

    @Autowired
    private TokenCipher tokenCipher;

    @MockitoBean
    private SocialConnectorFactory connectorFactory;

    private SocialMediaProvider connector;
    private SocialOAuthProvider oauthProvider;
    private UUID userId;
    private String basePath;

    @BeforeEach
    void setUp() {
        socialAccountRepository.deleteAll();
        credentialRepository.deleteAll();
        userRepository.deleteAll();

        userId = userRepository.save(User.builder().email("owner@example.com").fullName("Owner").build()).getId();
        basePath = "/api/v1/users/" + userId + "/social-accounts";

        connector = mock(SocialMediaProvider.class);
        oauthProvider = mock(SocialOAuthProvider.class);
        when(connectorFactory.getConnector(SocialPlatform.INSTAGRAM)).thenReturn(connector);
        when(connectorFactory.getOAuthProvider(SocialPlatform.INSTAGRAM)).thenReturn(oauthProvider);
        when(connector.connect(any())).thenReturn(SocialAuthResponse.builder()
                .authenticated(true)
                .platform(SocialPlatform.INSTAGRAM)
                .userId(ACCOUNT_ID)
                .username("alice")
                .build());
    }

    // ==================== Link / list / unlink ====================

    @Test
    @DisplayName("POST /{platform}/token: Should link the account and never expose or store the plain token")
    void linkWithToken_ShouldLinkAccount() throws Exception {
        // Act & Assert
        mockMvc.perform(post(basePath + "/instagram/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkSocialAccountDTO(TOKEN))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.platform").value("INSTAGRAM"))
                .andExpect(jsonPath("$.data.platformAccountId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(content().string(not(containsString(TOKEN))));

        List<SocialAccount> stored = socialAccountRepository.findAll();
        assertEquals(1, stored.size());
        assertNotEquals(TOKEN, stored.get(0).getAccessTokenEncrypted());
        assertFalse(stored.get(0).getAccessTokenEncrypted().contains(TOKEN));
    }

    @Test
    @DisplayName("GET / and DELETE /{id}: Should list and unlink accounts")
    void listAndUnlink_ShouldWork() throws Exception {
        // Arrange
        UUID accountId = linkAccount();

        // Act & Assert: list
        mockMvc.perform(get(basePath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(accountId.toString()));

        // Act & Assert: unlink
        mockMvc.perform(delete(basePath + "/" + accountId))
                .andExpect(status().isOk());

        mockMvc.perform(get(basePath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("DELETE /{id}: Should return 404 for an unknown account")
    void unlink_ShouldReturn404ForUnknownAccount() throws Exception {
        mockMvc.perform(delete(basePath + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SACC_001"));
    }

    @Test
    @DisplayName("Errors: Should map unknown user, unknown platform, and malformed IDs")
    void errors_ShouldMapToStatusCodes() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + UUID.randomUUID() + "/social-accounts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USR_001"));

        mockMvc.perform(post(basePath + "/myspace/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkSocialAccountDTO(TOKEN))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SC5001"));

        mockMvc.perform(get(basePath + "/not-a-uuid/posts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));

        mockMvc.perform(post(basePath + "/instagram/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SACC_002"));
    }

    // ==================== OAuth ====================

    @Test
    @DisplayName("OAuth: Should issue an authorization URL, then link the account from the callback")
    void oauthFlow_ShouldLinkAccount() throws Exception {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.of(2030, 1, 1, 0, 0);
        ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
        when(oauthProvider.buildAuthorizationUrl(stateCaptor.capture())).thenReturn("https://auth.example/authorize");
        when(oauthProvider.exchangeAuthorizationCode("code-1")).thenReturn(new SocialToken(TOKEN, expiresAt));

        // Act & Assert: start
        mockMvc.perform(post(basePath + "/instagram/authorize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorizationUrl").value("https://auth.example/authorize"));

        // Act & Assert: callback
        mockMvc.perform(get("/api/v1/social-accounts/oauth/instagram/callback")
                        .param("code", "code-1")
                        .param("state", stateCaptor.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformAccountId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.data.tokenExpiresAt").value("2030-01-01T00:00:00"));

        assertEquals(userId, socialAccountRepository.findAll().get(0).getUserId());
    }

    @Test
    @DisplayName("OAuth callback: Should reject forged state and report denial")
    void oauthCallback_ShouldRejectInvalidRequests() throws Exception {
        mockMvc.perform(get("/api/v1/social-accounts/oauth/instagram/callback")
                        .param("code", "code-1")
                        .param("state", "forged.state"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SACC_003"));

        mockMvc.perform(get("/api/v1/social-accounts/oauth/instagram/callback")
                        .param("error", "access_denied")
                        .param("error_description", "The user denied your request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SACC_004"));

        verify(oauthProvider, never()).exchangeAuthorizationCode(anyString());
    }

    // ==================== Data fetching ====================

    @Test
    @DisplayName("GET /{id}/posts: Should fetch posts with the decrypted token")
    void fetchPosts_ShouldProxyToConnector() throws Exception {
        // Arrange
        UUID accountId = linkAccount();
        when(connector.fetchPosts(any(), eq(ACCOUNT_ID), eq(5)))
                .thenReturn(List.of(SocialPost.builder().postId("m1").caption("Hello").build()));

        // Act & Assert
        mockMvc.perform(get(basePath + "/" + accountId + "/posts").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postId").value("m1"));

        ArgumentCaptor<SocialCredentials> captor = ArgumentCaptor.forClass(SocialCredentials.class);
        verify(connector).fetchPosts(captor.capture(), eq(ACCOUNT_ID), eq(5));
        assertEquals(TOKEN, captor.getValue().accessToken());
    }

    @Test
    @DisplayName("GET /{id}/posts: Should return 401 on a rejected token, then 409 until re-linked")
    void fetchPosts_ShouldRequireReauthAfterTokenRejected() throws Exception {
        // Arrange
        UUID accountId = linkAccount();
        when(connector.fetchPosts(any(), anyString(), anyInt()))
                .thenThrow(new SocialConnectorException(SocialConnectorErrorCode.TOKEN_EXPIRED));

        // Act & Assert
        mockMvc.perform(get(basePath + "/" + accountId + "/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SC1003"));

        mockMvc.perform(get(basePath + "/" + accountId + "/posts"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SACC_007"));

        mockMvc.perform(get(basePath))
                .andExpect(jsonPath("$.data[0].status").value("REAUTH_REQUIRED"));
    }

    @Test
    @DisplayName("GET /{id}/posts: Should retry once with a token refreshed while the request was in flight")
    void fetchPosts_ShouldRetryWithTokenRefreshedMidRequest() throws Exception {
        // Arrange
        UUID accountId = linkAccount();
        when(connector.fetchPosts(any(), anyString(), anyInt())).thenAnswer(invocation -> {
            SocialCredentials credentials = invocation.getArgument(0);
            if (TOKEN.equals(credentials.accessToken())) {
                // Simulate the refresh job committing a new token while this request holds the old one
                String current = socialAccountRepository.findTokenState(accountId).orElseThrow().accessTokenEncrypted();
                socialAccountRepository.updateTokenIfUnchanged(accountId, current, tokenCipher.encrypt("refreshed-token"),
                        LocalDateTime.of(2030, 1, 1, 0, 0), LocalDateTime.now(), LocalDateTime.now());
                throw new SocialConnectorException(SocialConnectorErrorCode.TOKEN_EXPIRED);
            }
            return List.of(SocialPost.builder().postId("m1").build());
        });

        // Act & Assert
        mockMvc.perform(get(basePath + "/" + accountId + "/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postId").value("m1"));

        ArgumentCaptor<SocialCredentials> captor = ArgumentCaptor.forClass(SocialCredentials.class);
        verify(connector, times(2)).fetchPosts(captor.capture(), anyString(), anyInt());
        assertEquals("refreshed-token", captor.getAllValues().get(1).accessToken());

        mockMvc.perform(get(basePath))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].tokenExpiresAt").value("2030-01-01T00:00:00"));
    }

    private UUID linkAccount() throws Exception {
        MvcResult result = mockMvc.perform(post(basePath + "/instagram/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkSocialAccountDTO(TOKEN))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }
}
