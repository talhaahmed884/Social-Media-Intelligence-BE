package com.media.intelligence.social_account.repository;

import com.media.intelligence.social_account.entity.SocialAccount;
import com.media.intelligence.social_account.entity.SocialAccountStatus;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SocialAccount entity.
 * "User_Id" navigates SocialAccount.user.id (same convention as UserCredentialRepository).
 */
@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

    List<SocialAccount> findByUser_IdOrderByCreatedAtAsc(UUID userId);

    /**
     * Find an account only if it belongs to the given user.
     */
    Optional<SocialAccount> findByIdAndUser_Id(UUID id, UUID userId);

    Optional<SocialAccount> findByUser_IdAndPlatformAndPlatformAccountId(
            UUID userId, SocialPlatform platform, String platformAccountId);

    /**
     * Accounts whose token should be refreshed: in the given status, old enough to be refreshable,
     * and either expiring before {@code expiresBefore} or with unknown expiry.
     */
    @Query("""
            select a from SocialAccount a
            where a.status = :status
              and a.tokenIssuedAt <= :issuedBefore
              and (a.tokenExpiresAt is null or a.tokenExpiresAt <= :expiresBefore)
            """)
    List<SocialAccount> findTokensDueForRefresh(@Param("status") SocialAccountStatus status,
                                                @Param("issuedBefore") LocalDateTime issuedBefore,
                                                @Param("expiresBefore") LocalDateTime expiresBefore);

    // ==================== Race-safe token access ====================
    // A request and the refresh job (or a re-link) can touch the same account concurrently.
    // These read fresh DB state and only write if the token is still the one the caller used,
    // so a stale copy can never overwrite a newer token or block a freshly refreshed account.

    /**
     * Read the account's current token state from the database (bypasses the persistence context).
     */
    @Query("""
            select new com.media.intelligence.social_account.repository.SocialAccountTokenState(
                a.accessTokenEncrypted, a.tokenExpiresAt, a.status)
            from SocialAccount a
            where a.id = :id
            """)
    Optional<SocialAccountTokenState> findTokenState(@Param("id") UUID id);

    /**
     * Set the status only if the stored token is still {@code expectedToken}.
     *
     * @return rows updated (0 if the token changed meanwhile or the account is gone)
     */
    @Transactional
    @Modifying
    @Query("""
            update SocialAccount a
            set a.status = :status, a.updatedAt = :updatedAt
            where a.id = :id and a.accessTokenEncrypted = :expectedToken
            """)
    int updateStatusIfTokenUnchanged(@Param("id") UUID id,
                                     @Param("expectedToken") String expectedToken,
                                     @Param("status") SocialAccountStatus status,
                                     @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Replace the token only if the stored token is still {@code expectedToken}.
     *
     * @return rows updated (0 if the token changed meanwhile or the account is gone)
     */
    @Transactional
    @Modifying
    @Query("""
            update SocialAccount a
            set a.accessTokenEncrypted = :newToken,
                a.tokenExpiresAt = :expiresAt,
                a.tokenIssuedAt = :issuedAt,
                a.updatedAt = :updatedAt
            where a.id = :id and a.accessTokenEncrypted = :expectedToken
            """)
    int updateTokenIfUnchanged(@Param("id") UUID id,
                               @Param("expectedToken") String expectedToken,
                               @Param("newToken") String newToken,
                               @Param("expiresAt") LocalDateTime expiresAt,
                               @Param("issuedAt") LocalDateTime issuedAt,
                               @Param("updatedAt") LocalDateTime updatedAt);
}
