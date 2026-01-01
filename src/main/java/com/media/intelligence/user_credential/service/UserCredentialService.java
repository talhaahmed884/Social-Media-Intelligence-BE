package com.media.intelligence.user_credential.service;

import com.media.intelligence.user.dto.ChangePasswordDTO;
import com.media.intelligence.user.entity.User;
import com.media.intelligence.user.exception.UserErrorCode;
import com.media.intelligence.user.exception.UserException;
import com.media.intelligence.user.sanitization.ChangePasswordDTOSanitizer;
import com.media.intelligence.user.validation.ChangePasswordDTOValidator;
import com.media.intelligence.user_credential.entity.UserCredential;
import com.media.intelligence.user_credential.exception.UserCredentialErrorCode;
import com.media.intelligence.user_credential.exception.UserCredentialException;
import com.media.intelligence.user_credential.repository.UserCredentialRepository;
import com.media.intelligence.user_credential.strategy.PasswordHashingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing user credentials (password hashing and verification).
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
        // Validate user is not null
        if (user == null) {
            log.error("Credential creation failed: user is null");
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_CREATION_FAILED);
        }

        UUID userId = user.getId();

        // Validate password is not null or empty
        if (password == null || password.trim().isEmpty()) {
            log.error("Credential creation failed: password is null or empty");
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_REQUIRED);
        }

        log.debug("Creating credentials for user: {}", userId);

        try {
            // Check if credentials already exist
            if (credentialRepository.existsByUser_Id(userId)) {
                log.warn("Credential creation failed: credentials already exist for user: {}", userId);
                throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_CREATION_FAILED);
            }

            // Hash the password
            String passwordHash;
            try {
                passwordHash = hashingStrategy.hash(password);
            } catch (Exception e) {
                log.error("Failed to hash password for user {}: {}", userId, e.getMessage(), e);
                throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_HASH_FAILED);
            }

            // Build credential entity using the provided User object
            UserCredential credential = UserCredential.builder()
                    .user(user)
                    .passwordHash(passwordHash)
                    .build();

            // Save to database
            UserCredential saved;
            try {
                saved = credentialRepository.save(credential);
                log.info("Credentials created successfully for user: {}", userId);
            } catch (Exception e) {
                log.error("Failed to save credentials to database for user {}: {}", userId, e.getMessage(), e);
                throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_CREATION_FAILED);
            }

            return saved;

        } catch (UserCredentialException e) {
            // Re-throw UserCredentialException as-is
            throw e;
        } catch (Exception e) {
            // Catch any unexpected exceptions
            log.error("Unexpected error during credential creation for user {}: {}", userId, e.getMessage(), e);
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_CREATION_FAILED);
        }
    }

    /**
     * Verify password for a user.
     *
     * @param userId   the user ID
     * @param password the plaintext password
     * @return true if password is correct
     * @throws UserCredentialException if credentials not found, userId is null, or password is invalid
     */
    public boolean verifyPassword(UUID userId, String password) {
        // Validate userId is not null
        if (userId == null) {
            log.error("Password verification failed: userId is null");
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND);
        }

        // Validate password is not null or empty
        if (password == null || password.trim().isEmpty()) {
            log.error("Password verification failed: password is null or empty");
            throw new UserCredentialException(UserCredentialErrorCode.INVALID_PASSWORD);
        }

        try {
            // Get credential from database
            UserCredential credential = getCredentialByUserId(userId);

            // Verify password
            boolean isValid;
            try {
                isValid = hashingStrategy.verify(password, credential.getPasswordHash());
                log.debug("Password verification for user {}: {}", userId, isValid ? "success" : "failed");
            } catch (Exception e) {
                log.error("Failed to verify password for user {}: {}", userId, e.getMessage(), e);
                throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_VERIFICATION_FAILED);
            }

            return isValid;

        } catch (UserCredentialException e) {
            // Re-throw UserCredentialException as-is
            throw e;
        } catch (Exception e) {
            // Catch any unexpected exceptions
            log.error("Unexpected error during password verification for user {}: {}", userId, e.getMessage(), e);
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_VERIFICATION_FAILED);
        }
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
        // Validate userId is not null
        if (userId == null) {
            log.error("Password update failed: userId is null");
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_UPDATE_FAILED);
        }

        // Validate newPassword is not null or empty
        if (newPassword == null || newPassword.trim().isEmpty()) {
            log.error("Password update failed: newPassword is null or empty");
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_REQUIRED);
        }

        log.debug("Updating password for user: {}", userId);

        try {
            // Get existing credential
            UserCredential credential = getCredentialByUserId(userId);

            // Hash the new password
            String newPasswordHash;
            try {
                newPasswordHash = hashingStrategy.hash(newPassword);
            } catch (Exception e) {
                log.error("Failed to hash new password for user {}: {}", userId, e.getMessage(), e);
                throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_HASH_FAILED);
            }

            // Update and save
            credential.setPasswordHash(newPasswordHash);
            try {
                credentialRepository.save(credential);
                log.info("Password updated successfully for user: {}", userId);
            } catch (Exception e) {
                log.error("Failed to save updated credentials for user {}: {}", userId, e.getMessage(), e);
                throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_UPDATE_FAILED);
            }

        } catch (UserCredentialException e) {
            // Re-throw UserCredentialException as-is
            throw e;
        } catch (Exception e) {
            // Catch any unexpected exceptions
            log.error("Unexpected error during password update for user {}: {}", userId, e.getMessage(), e);
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_UPDATE_FAILED);
        }
    }

    /**
     * Get credential by user ID.
     *
     * @param userId the user ID
     * @return the credential
     * @throws UserCredentialException if not found or userId is null
     */
    public UserCredential getCredentialByUserId(UUID userId) {
        // Validate userId is not null
        if (userId == null) {
            log.error("Get credential failed: userId is null");
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND);
        }

        try {
            return credentialRepository.findByUser_Id(userId)
                    .orElseThrow(() -> {
                        log.warn("Credential not found for userId: {}", userId);
                        return new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND);
                    });
        } catch (UserCredentialException e) {
            // Re-throw UserCredentialException as-is
            throw e;
        } catch (Exception e) {
            // Catch any unexpected database errors
            log.error("Unexpected error while getting credential for user {}: {}", userId, e.getMessage(), e);
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND);
        }
    }

    /**
     * Change password for a user.
     * Verifies current password before updating to new password.
     *
     * @param userId the user ID
     * @param dto    the current plaintext password and the new plaintext password
     * @throws UserCredentialException if current password is incorrect or update fails
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordDTO dto) {
        // Sanitize and validate in controller for password change
        changePasswordSanitizer.sanitize(dto);
        var validationResult = changePasswordValidator.validate(dto);

        if (validationResult.hasErrors()) {
            log.warn("Password change validation failed: {}", validationResult.getDetailedErrorMessage());
            throw new UserException(UserErrorCode.INVALID_INPUT);
        }

        // Validate inputs
        if (userId == null) {
            log.error("Password change failed: userId is null");
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_UPDATE_FAILED);
        }

        log.info("Attempting to change password for user: {}", userId);

        try {
            // Step 1: Verify current password
            boolean isCurrentPasswordValid = verifyPassword(userId, dto.getCurrentPassword());
            if (!isCurrentPasswordValid) {
                log.warn("Password change failed: current password is incorrect for user: {}", userId);
                throw new UserCredentialException(UserCredentialErrorCode.CURRENT_PASSWORD_MISMATCH);
            }

            // Step 2: Update to new password
            updatePassword(userId, dto.getNewPassword());
            log.info("Password changed successfully for user: {}", userId);

        } catch (UserCredentialException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during password change for user {}: {}", userId, e.getMessage(), e);
            throw new UserCredentialException(UserCredentialErrorCode.CREDENTIAL_UPDATE_FAILED);
        }
    }
}
