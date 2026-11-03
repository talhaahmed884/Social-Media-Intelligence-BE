package com.media.intelligence.user_credential.service;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.user.entity.User;
import com.media.intelligence.user_credential.dto.ChangePasswordDTO;
import com.media.intelligence.user_credential.entity.UserCredential;
import com.media.intelligence.user_credential.exception.UserCredentialErrorCode;
import com.media.intelligence.user_credential.exception.UserCredentialException;
import com.media.intelligence.user_credential.repository.UserCredentialRepository;
import com.media.intelligence.user_credential.sanitization.ChangePasswordDTOSanitizer;
import com.media.intelligence.user_credential.strategy.PasswordHashingStrategy;
import com.media.intelligence.user_credential.validation.ChangePasswordDTOValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing user credentials (password hashing and verification).
 * <p>
 * UserCredentialExceptions propagate unchanged. Hashing/verification failures from the strategy are
 * reported as PASSWORD_HASH_FAILED / PASSWORD_VERIFICATION_FAILED; other unexpected failures (e.g. database
 * outages) propagate to the GlobalExceptionHandler as 500 instead of being relabelled as "not found".
 * <p>
 * After a successful verification, hashes in a legacy format (SHA-512) or with an outdated BCrypt cost
 * are transparently re-hashed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCredentialService {
    private final UserCredentialRepository credentialRepository;
    private final PasswordHashingStrategy hashingStrategy;
    private final ChangePasswordDTOValidator changePasswordValidator;
    private final ChangePasswordDTOSanitizer changePasswordSanitizer;

    /**
     * Create credentials for a new user.
     *
     * @param user     the user entity
     * @param password the plaintext password
     * @return the created credential
     * @throws UserCredentialException if creation fails, user is null, or password is invalid
     */
    @Transactional
    public UserCredential createCredential(User user, String password) {
        if (user == null) {
            log.error("Credential creation failed: user is null");
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_CREATION_FAILED);
        }

        UUID userId = user.getId();

        if (password == null || password.trim().isEmpty()) {
            log.error("Credential creation failed: password is null or empty");
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_REQUIRED);
        }

        log.debug("Creating credentials for user: {}", userId);

        if (credentialRepository.existsByUser_Id(userId)) {
            log.warn("Credential creation failed: credentials already exist for user: {}", userId);
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_CREATION_FAILED);
        }

        UserCredential credential = UserCredential.builder()
                .user(user)
                .passwordHash(hash(password, userId))
                .build();

        UserCredential saved = credentialRepository.save(credential);
        log.info("Credentials created successfully for user: {}", userId);
        return saved;
    }

    /**
     * Verify password for a user. On success, upgrades the stored hash if the strategy asks for it.
     *
     * @param userId   the user ID
     * @param password the plaintext password
     * @return true if password is correct
     * @throws UserCredentialException if credentials not found, userId is null, or password is invalid
     */
    public boolean verifyPassword(UUID userId, String password) {
        if (userId == null) {
            log.error("Password verification failed: userId is null");
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND);
        }

        if (password == null || password.trim().isEmpty()) {
            log.error("Password verification failed: password is null or empty");
            throw new UserCredentialException(UserCredentialErrorCode.INVALID_PASSWORD);
        }

        UserCredential credential = getCredentialByUserId(userId);

        boolean isValid;
        try {
            isValid = hashingStrategy.verify(password, credential.getPasswordHash());
        } catch (UserCredentialException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Failed to verify password for user {}: {}", userId, e.getMessage(), e);
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_VERIFICATION_FAILED, e);
        }
        log.debug("Password verification for user {}: {}", userId, isValid ? "success" : "failed");

        if (isValid) {
            upgradeHashIfNeeded(credential, password);
        }
        return isValid;
    }

    /**
     * Update password for a user.
     *
     * @param userId      the user ID
     * @param newPassword the new plaintext password
     * @throws UserCredentialException if update fails, userId is null, or password is invalid
     */
    @Transactional
    public void updatePassword(UUID userId, String newPassword) {
        if (userId == null) {
            log.error("Password update failed: userId is null");
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_UPDATE_FAILED);
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            log.error("Password update failed: newPassword is null or empty");
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_REQUIRED);
        }

        log.debug("Updating password for user: {}", userId);

        UserCredential credential = getCredentialByUserId(userId);
        credential.setPasswordHash(hash(newPassword, userId));
        credentialRepository.save(credential);

        log.info("Password updated successfully for user: {}", userId);
    }

    /**
     * Get credential by user ID.
     *
     * @param userId the user ID
     * @return the credential
     * @throws UserCredentialException if not found or userId is null
     */
    public UserCredential getCredentialByUserId(UUID userId) {
        if (userId == null) {
            log.error("Get credential failed: userId is null");
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND);
        }

        return credentialRepository.findByUser_Id(userId)
                .orElseThrow(() -> {
                    log.warn("Credential not found for userId: {}", userId);
                    return new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND);
                });
    }

    /**
     * Change password for a user.
     * Verifies current password before updating to new password.
     *
     * @param userId the user ID
     * @param dto    the current plaintext password and the new plaintext password
     * @throws UserCredentialException if input is invalid (CRED_013 with field errors), the current password
     *                                 is incorrect, or the update fails
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordDTO dto) {
        changePasswordSanitizer.sanitize(dto);
        ValidationResult validationResult = changePasswordValidator.validate(dto);

        if (validationResult.hasErrors()) {
            log.warn("Password change validation failed: {}", validationResult.getDetailedErrorMessage());
            throw new UserCredentialException(UserCredentialErrorCode.INVALID_INPUT, validationResult.getFormattedErrors());
        }

        if (userId == null) {
            log.error("Password change failed: userId is null");
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_UPDATE_FAILED);
        }

        log.info("Attempting to change password for user: {}", userId);

        if (!verifyPassword(userId, dto.getCurrentPassword())) {
            log.warn("Password change failed: current password is incorrect for user: {}", userId);
            throw new UserCredentialException(UserCredentialErrorCode.CURRENT_PASSWORD_MISMATCH);
        }

        updatePassword(userId, dto.getNewPassword());
        log.info("Password changed successfully for user: {}", userId);
    }

    /**
     * Hash with the strategy; strategy-level UserCredentialExceptions (e.g. PASSWORD_TOO_LONG) propagate,
     * anything else becomes PASSWORD_HASH_FAILED.
     */
    private String hash(String password, UUID userId) {
        try {
            return hashingStrategy.hash(password);
        } catch (UserCredentialException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Failed to hash password for user {}: {}", userId, e.getMessage(), e);
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_HASH_FAILED, e);
        }
    }

    /**
     * Re-hash a just-verified password if its stored hash is legacy or outdated. Failures are logged and
     * ignored: the user already proved the password, and the next verification will try again.
     */
    private void upgradeHashIfNeeded(UserCredential credential, String password) {
        if (!hashingStrategy.needsRehash(credential.getPasswordHash())) {
            return;
        }
        try {
            credential.setPasswordHash(hashingStrategy.hash(password));
            credentialRepository.save(credential);
            log.info("Upgraded password hash for user: {}", credential.getUserId());
        } catch (RuntimeException e) {
            log.warn("Could not upgrade password hash for user {}: {}", credential.getUserId(), e.getMessage());
        }
    }
}
