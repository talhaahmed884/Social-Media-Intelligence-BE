package com.media.intelligence.user_credential.repository;

import com.media.intelligence.user_credential.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserCredential entity.
 */
@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {

    /**
     * Find user credentials by user ID.
     *
     * @param userId the user ID
     * @return optional user credential
     */
    Optional<UserCredential> findByUserId(UUID userId);

    /**
     * Check if credentials exist for a user.
     *
     * @param userId the user ID
     * @return true if credentials exist
     */
    boolean existsByUserId(UUID userId);

    /**
     * Delete credentials by user ID.
     *
     * @param userId the user ID
     */
    void deleteByUserId(UUID userId);
}
