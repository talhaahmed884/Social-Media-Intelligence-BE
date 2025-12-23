package com.media.intelligence.user.service;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.user.dto.FindUserByEmailDTO;
import com.media.intelligence.user.dto.RegisterUserDTO;
import com.media.intelligence.user.entity.User;
import com.media.intelligence.user.exception.UserErrorCode;
import com.media.intelligence.user.exception.UserException;
import com.media.intelligence.user.repository.UserRepository;
import com.media.intelligence.user.sanitization.FindUserByEmailDTOSanitizer;
import com.media.intelligence.user.sanitization.RegisterUserDTOSanitizer;
import com.media.intelligence.user.validation.FindUserByEmailDTOValidator;
import com.media.intelligence.user.validation.RegisterUserDTOValidator;
import com.media.intelligence.user_credential.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for user registration in the User Domain.
 * <p>
 * Key responsibilities:
 * - Sanitize and validate user input
 * - Enforce business rules (email uniqueness)
 * - Hash passwords securely
 * - Throw domain-specific UserExceptions for business rule violations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {
    private final UserRepository userRepository;
    private final UserCredentialService credentialService;
    private final RegisterUserDTOValidator registerUserValidator;
    private final RegisterUserDTOSanitizer registerUserSanitizer;
    private final FindUserByEmailDTOValidator findUserByEmailValidator;
    private final FindUserByEmailDTOSanitizer findUserByEmailSanitizer;

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
        // Validate DTO is not null
        if (dto == null) {
            log.error("Registration failed: DTO is null");
            throw new UserException(UserErrorCode.INVALID_INPUT);
        }

        log.info("Attempting to register user with email: {}", dto.getEmail());

        try {
            // Step 1: Sanitize the DTO (modifies in place)
            registerUserSanitizer.sanitize(dto);
            log.debug("DTO sanitized: email={}, fullName={}", dto.getEmail(), dto.getFullName());

            // Step 2: Validate the sanitized DTO
            ValidationResult validationResult = registerUserValidator.validate(dto);
            if (validationResult.hasErrors()) {
                log.warn("Registration validation failed: {}", validationResult.getDetailedErrorMessage());
                throw new UserException(UserErrorCode.INVALID_INPUT);
            }

            // Business Rule: Email must be unique
            if (userRepository.existsByEmail(dto.getEmail())) {
                log.warn("Registration failed: Email already exists: {}", dto.getEmail());
                throw new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS);
            }

            // Create user entity (without password)
            User user = User.builder()
                    .email(dto.getEmail())           // Already sanitized and normalized
                    .fullName(dto.getFullName())     // Already sanitized
                    .build();

            // Persist user
            User savedUser;
            try {
                savedUser = userRepository.save(user);
                log.info("User registered successfully with ID: {}", savedUser.getId());
            } catch (Exception e) {
                log.error("Failed to save user to database: {}", e.getMessage(), e);
                throw new UserException(UserErrorCode.REGISTRATION_FAILED);
            }

            // Create credentials in separate table
            try {
                credentialService.createCredential(savedUser.getId(), dto.getPassword());
                log.debug("Credentials created for user: {}", savedUser.getId());
            } catch (Exception e) {
                log.error("Failed to create credentials for user {}: {}", savedUser.getId(), e.getMessage(), e);
                throw new UserException(UserErrorCode.REGISTRATION_FAILED);
            }

            return savedUser;

        } catch (UserException e) {
            // Re-throw UserException as-is
            throw e;
        } catch (Exception e) {
            // Catch any unexpected exceptions
            log.error("Unexpected error during user registration: {}", e.getMessage(), e);
            throw new UserException(UserErrorCode.REGISTRATION_FAILED);
        }
    }

    /**
     * Find user by ID.
     *
     * @param userId the user ID
     * @return the user
     * @throws UserException if user not found or userId is null
     */
    public User findUserById(UUID userId) {
        // Validate userId is not null
        if (userId == null) {
            log.error("Find user failed: userId is null");
            throw new UserException(UserErrorCode.USER_ID_INVALID);
        }

        try {
            return userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("User not found for userId: {}", userId);
                        return new UserException(UserErrorCode.USER_NOT_FOUND);
                    });
        } catch (UserException e) {
            // Re-throw UserException as-is
            throw e;
        } catch (Exception e) {
            // Catch any unexpected database errors
            log.error("Unexpected error while finding user by ID {}: {}", userId, e.getMessage(), e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
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
        // Validate DTO is not null
        if (dto == null) {
            log.error("Find user by email failed: DTO is null");
            throw new UserException(UserErrorCode.INVALID_INPUT);
        }

        log.debug("Finding user by email: {}", dto.getEmail());

        try {
            // Step 1: Sanitize the DTO (modifies in place)
            findUserByEmailSanitizer.sanitize(dto);
            log.debug("DTO sanitized: email={}", dto.getEmail());

            // Step 2: Validate the sanitized DTO
            ValidationResult validationResult = findUserByEmailValidator.validate(dto);
            if (validationResult.hasErrors()) {
                log.warn("Find user by email validation failed: {}", validationResult.getDetailedErrorMessage());
                throw new UserException(UserErrorCode.INVALID_EMAIL_FORMAT);
            }

            // Step 3: Find user
            return userRepository.findByEmail(dto.getEmail())
                    .orElseThrow(() -> {
                        log.warn("User not found for email: {}", dto.getEmail());
                        return new UserException(UserErrorCode.USER_NOT_FOUND);
                    });

        } catch (UserException e) {
            // Re-throw UserException as-is
            throw e;
        } catch (Exception e) {
            // Catch any unexpected database errors
            log.error("Unexpected error while finding user by email: {}", e.getMessage(), e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
    }
}
