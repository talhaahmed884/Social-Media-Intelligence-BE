package com.media.intelligence.social_account.repository;

import com.media.intelligence.social_account.entity.SocialAccount;
import com.media.intelligence.social_account.entity.SocialAccountStatus;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import com.media.intelligence.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SocialAccountRepository Integration Tests")
public class SocialAccountRepositoryIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Autowired
    private SocialAccountRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = entityManager.persist(User.builder().email("owner@example.com").fullName("Owner").build());
        otherUser = entityManager.persist(User.builder().email("other@example.com").fullName("Other").build());
        entityManager.flush();
    }

    @Test
    @DisplayName("findByUser_Id: Should return only the user's accounts")
    void findByUserId_ShouldReturnOwnedAccounts() {
        // Arrange
        SocialAccount first = repository.saveAndFlush(account(owner, "acc-1", NOW.plusDays(30), NOW));
        SocialAccount second = repository.saveAndFlush(account(owner, "acc-2", NOW.plusDays(30), NOW));
        repository.saveAndFlush(account(otherUser, "acc-3", NOW.plusDays(30), NOW));

        // Act
        List<SocialAccount> accounts = repository.findByUser_IdOrderByCreatedAtAsc(owner.getId());

        // Assert (creation timestamps can tie, so compare as a set)
        assertEquals(Set.of(first.getId(), second.getId()), accounts.stream().map(SocialAccount::getId).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("findByIdAndUser_Id: Should not return another user's account")
    void findByIdAndUserId_ShouldEnforceOwnership() {
        // Arrange
        SocialAccount account = repository.saveAndFlush(account(owner, "acc-1", null, NOW));

        // Act & Assert
        assertTrue(repository.findByIdAndUser_Id(account.getId(), owner.getId()).isPresent());
        assertTrue(repository.findByIdAndUser_Id(account.getId(), otherUser.getId()).isEmpty());
    }

    @Test
    @DisplayName("save: Should reject linking the same platform account twice for one user")
    void save_ShouldEnforceUniquePerUser() {
        // Arrange
        repository.saveAndFlush(account(owner, "acc-1", null, NOW));

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () ->
                repository.saveAndFlush(account(owner, "acc-1", null, NOW)));
    }

    @Test
    @DisplayName("save: Should allow different users to link the same platform account")
    void save_ShouldAllowSameAccountForDifferentUsers() {
        repository.saveAndFlush(account(owner, "acc-1", null, NOW));

        assertDoesNotThrow(() -> repository.saveAndFlush(account(otherUser, "acc-1", null, NOW)));
    }

    @Test
    @DisplayName("findTokensDueForRefresh: Should select old-enough active tokens expiring soon or with unknown expiry")
    void findTokensDueForRefresh_ShouldSelectDueAccounts() {
        // Arrange
        LocalDateTime issuedBefore = NOW.minusHours(24);
        LocalDateTime expiresBefore = NOW.plusDays(10);

        SocialAccount expiringSoon = repository.save(account(owner, "due-1", NOW.plusDays(5), NOW.minusDays(55)));
        SocialAccount unknownExpiry = repository.save(account(owner, "due-2", null, NOW.minusDays(2)));
        repository.save(account(owner, "far-expiry", NOW.plusDays(50), NOW.minusDays(10)));
        repository.save(account(owner, "too-new", NOW.plusDays(5), NOW.minusHours(1)));
        SocialAccount needsReauth = account(owner, "reauth", NOW.plusDays(5), NOW.minusDays(55));
        needsReauth.setStatus(SocialAccountStatus.REAUTH_REQUIRED);
        repository.save(needsReauth);
        repository.flush();

        // Act
        List<SocialAccount> due = repository.findTokensDueForRefresh(SocialAccountStatus.ACTIVE, issuedBefore, expiresBefore);

        // Assert
        assertEquals(
                Set.of(expiringSoon.getId(), unknownExpiry.getId()),
                due.stream().map(SocialAccount::getId).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("conditional updates: Should apply only while the stored token is unchanged, and read fresh state")
    void conditionalUpdates_ShouldGuardOnToken() {
        // Arrange
        SocialAccount account = repository.saveAndFlush(account(owner, "acc-1", null, NOW));
        UUID id = account.getId();

        // Act & Assert: stale token -> no-op
        assertEquals(0, repository.updateStatusIfTokenUnchanged(id, "stale", SocialAccountStatus.REAUTH_REQUIRED, NOW));
        assertEquals(0, repository.updateTokenIfUnchanged(id, "stale", "new", NOW, NOW, NOW));

        // Act & Assert: matching token -> applied, visible through findTokenState despite the cached entity
        assertEquals(1, repository.updateTokenIfUnchanged(id, "encrypted", "rotated", NOW.plusDays(60), NOW, NOW));
        assertEquals(1, repository.updateStatusIfTokenUnchanged(id, "rotated", SocialAccountStatus.REAUTH_REQUIRED, NOW));

        SocialAccountTokenState state = repository.findTokenState(id).orElseThrow();
        assertEquals("rotated", state.accessTokenEncrypted());
        assertEquals(NOW.plusDays(60), state.tokenExpiresAt());
        assertEquals(SocialAccountStatus.REAUTH_REQUIRED, state.status());
        assertTrue(repository.findTokenState(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("delete user: Should cascade to linked social accounts")
    void deleteUser_ShouldCascadeToAccounts() {
        // Arrange
        SocialAccount account = repository.saveAndFlush(account(owner, "acc-1", null, NOW));
        UUID accountId = account.getId();
        entityManager.clear();

        // Act
        entityManager.remove(entityManager.find(User.class, owner.getId()));
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertTrue(repository.findById(accountId).isEmpty());
    }

    private static SocialAccount account(User user, String platformAccountId,
                                         LocalDateTime expiresAt, LocalDateTime issuedAt) {
        return SocialAccount.builder()
                .user(user)
                .platform(SocialPlatform.INSTAGRAM)
                .platformAccountId(platformAccountId)
                .username("user-" + platformAccountId)
                .accessTokenEncrypted("encrypted")
                .tokenExpiresAt(expiresAt)
                .tokenIssuedAt(issuedAt)
                .status(SocialAccountStatus.ACTIVE)
                .build();
    }
}
