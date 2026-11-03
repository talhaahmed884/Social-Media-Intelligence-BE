package com.media.intelligence.user.service;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.user.dto.FindUserByEmailDTO;
import com.media.intelligence.user.dto.RegisterUserDTO;
import com.media.intelligence.user.dto.UpdateUserProfileDTO;
import com.media.intelligence.user.entity.User;
import com.media.intelligence.user.exception.UserErrorCode;
import com.media.intelligence.user.exception.UserException;
import com.media.intelligence.user.repository.UserRepository;
import com.media.intelligence.user.sanitization.FindUserByEmailDTOSanitizer;
import com.media.intelligence.user.sanitization.RegisterUserDTOSanitizer;
import com.media.intelligence.user.sanitization.UpdateUserProfileDTOSanitizer;
import com.media.intelligence.user.validation.FindUserByEmailDTOValidator;
import com.media.intelligence.user.validation.RegisterUserDTOValidator;
import com.media.intelligence.user.validation.UpdateUserProfileDTOValidator;
import com.media.intelligence.user_credential.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for user management in the User Domain.
 * <p>
 * Key responsibilities:
 * - Sanitize and validate user input (field errors are returned to the client in ErrorResponse.errors)
 * - Enforce business rules (email uniqueness, also under concurrent requests)
 * - Delegate password storage to UserCredentialService
 * - Throw domain-specific UserExceptions for business rule violations
 * <p>
 * Domain exceptions (UserException, UserCredentialException) propagate unchanged. Unexpected failures
 * (e.g. database outages) are not relabelled as "not found"; they propagate to the GlobalExceptionHandler
 * and return 500 INTERNAL_SERVER_ERROR.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserCredentialService credentialService;
    private final RegisterUserDTOValidator registerUserValidator;
    private final RegisterUserDTOSanitizer registerUserSanitizer;
    private final FindUserByEmailDTOValidator findUserByEmailValidator;
    private final FindUserByEmailDTOSanitizer findUserByEmailSanitizer;
    private final UpdateUserProfileDTOValidator updateUserProfileValidator;
    private final UpdateUserProfileDTOSanitizer updateUserProfileSanitizer;

    /**
     * Register a new user.
     * <p>
     * Sanitizes and validates the input DTO before processing.
     *
     * @param dto the registration data
     * @return the created user
     * @throws UserException if validation fails or email already exists
     */
    @Transactional
    public User registerUser(RegisterUserDTO dto) {
        if (dto == null) {
            log.error("Registration failed: DTO is null");
            throw new UserException(UserErrorCode.INVALID_INPUT);
        }

        log.info("Attempting to register user with email: {}", dto.getEmail());

        // Step 1: Sanitize (modifies in place), then validate
        registerUserSanitizer.sanitize(dto);
        requireValid(registerUserValidator.validate(dto), UserErrorCode.INVALID_INPUT, "Registration");

        // Step 2: Business rule - email must be unique
        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warn("Registration failed: Email already exists: {}", dto.getEmail());
            throw new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Step 3: Persist user, then credentials in the same transaction
        User savedUser = saveEnforcingUniqueEmail(User.builder()
                .email(dto.getEmail())           // Already sanitized and normalized
                .fullName(dto.getFullName())     // Already sanitized
                .build());

        credentialService.createCredential(savedUser, dto.getPassword());

        log.info("User registered successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    /**
     * Find user by ID.
     *
     * @param userId the user ID
     * @return the user
     * @throws UserException if user not found or userId is null
     */
    public User findUserById(UUID userId) {
        if (userId == null) {
            log.error("Find user failed: userId is null");
            throw new UserException(UserErrorCode.USER_ID_INVALID);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for userId: {}", userId);
                    return new UserException(UserErrorCode.USER_NOT_FOUND);
                });
    }

    /**
     * Find user by email.
     * <p>
     * Sanitizes and validates the input DTO before lookup.
     *
     * @param dto the find user by email data
     * @return the user
     * @throws UserException if validation fails or user not found
     */
    public User findUserByEmail(FindUserByEmailDTO dto) {
        if (dto == null) {
            log.error("Find user by email failed: DTO is null");
            throw new UserException(UserErrorCode.INVALID_INPUT);
        }

        findUserByEmailSanitizer.sanitize(dto);
        requireValid(findUserByEmailValidator.validate(dto), UserErrorCode.INVALID_EMAIL_FORMAT, "Find user by email");

        return userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", dto.getEmail());
                    return new UserException(UserErrorCode.USER_NOT_FOUND);
                });
    }

    /**
     * Get all users.
     *
     * @return list of all users
     */
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Update user profile.
     * <p>
     * Sanitizes and validates the input DTO before updating.
     *
     * @param userId the user ID
     * @param dto    the update profile data
     * @return the updated user
     * @throws UserException if validation fails, user not found, or email already exists
     */
    @Transactional
    public User updateUserProfile(UUID userId, UpdateUserProfileDTO dto) {
        if (userId == null) {
            log.error("Update user failed: userId is null");
            throw new UserException(UserErrorCode.USER_ID_INVALID);
        }

        if (dto == null) {
            log.error("Update user failed: DTO is null");
            throw new UserException(UserErrorCode.INVALID_INPUT);
        }

        log.info("Attempting to update user profile for userId: {}", userId);

        // Step 1: Sanitize, then validate
        updateUserProfileSanitizer.sanitize(dto);
        requireValid(updateUserProfileValidator.validate(dto), UserErrorCode.INVALID_INPUT, "Update profile");

        // Step 2: Find the existing user
        User existingUser = findUserById(userId);

        // Step 3: Business rule - a changed email must not belong to another user
        if (!existingUser.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            log.warn("Update failed: Email already exists: {}", dto.getEmail());
            throw new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Step 4: Update and save
        existingUser.setEmail(dto.getEmail());
        existingUser.setFullName(dto.getFullName());
        User updatedUser = saveEnforcingUniqueEmail(existingUser);

        log.info("User profile updated successfully for userId: {}", userId);
        return updatedUser;
    }

    /**
     * Delete user by ID.
     * Also deletes associated credentials (CASCADE delete via FK constraint).
     *
     * @param userId the user ID
     * @throws UserException if user not found
     */
    @Transactional
    public void deleteUser(UUID userId) {
        if (userId == null) {
            log.error("Delete user failed: userId is null");
            throw new UserException(UserErrorCode.USER_ID_INVALID);
        }

        log.info("Attempting to delete user with userId: {}", userId);

        User user = findUserById(userId);
        userRepository.delete(user);

        log.info("User deleted successfully: {}", userId);
    }

    /**
     * Throw a UserException carrying the field errors if validation failed.
     */
    private void requireValid(ValidationResult result, UserErrorCode errorCode, String operation) {
        if (result.hasErrors()) {
            log.warn("{} validation failed: {}", operation, result.getDetailedErrorMessage());
            throw new UserException(errorCode, result.getFormattedErrors());
        }
    }

    /**
     * Save and flush so a unique-email violation from a concurrent request surfaces here
     * (as EMAIL_ALREADY_EXISTS) instead of as a 500 at commit time.
     */
    private User saveEnforcingUniqueEmail(User user) {
        try {
            User saved = userRepository.save(user);
            userRepository.flush();
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Save failed on a constraint (concurrent duplicate email?): {}", user.getEmail());
            throw new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS, e);
        }
    }
}
