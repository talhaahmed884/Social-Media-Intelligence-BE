package com.media.intelligence.user_credential.repository;

import com.media.intelligence.user_credential.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserCredential entity.
 * <p>
 * Note: Methods use "User_Id" to navigate the relationship:
 * - "user" is the field name in UserCredential entity
 * - "id" is the field name in User entity
 * - Spring Data JPA uses underscore to navigate nested properties
 */
@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
    /**
     * Find user credentials by user ID.
     * Navigates: UserCredential.user.id
     *
     * @param userId the user ID
     * @return optional user credential
     */
    Optional<UserCredential> findByUser_Id(UUID userId);

    /**
     * Check if credentials exist for a user.
     * Navigates: UserCredential.user.id
     *
     * @param userId the user ID
     * @return true if credentials exist
     */
    boolean existsByUser_Id(UUID userId);

    /**
     * Delete credentials by user ID.
     * Navigates: UserCredential.user.id
     *
     * @param userId the user ID
     */
    void deleteByUser_Id(UUID userId);
}
