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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
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
        log.info("Attempting to register user with email: {}", dto.getEmail());

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

        // Hash password (TODO: Replace with BCrypt in production)
        String passwordHash = hashPassword(dto.getPassword());

        // Create user entity
        User user = User.builder()
                .email(dto.getEmail())           // Already sanitized and normalized
                .passwordHash(passwordHash)
                .fullName(dto.getFullName())     // Already sanitized
                .isActive(true)
                .isLocked(false)
                .failedLoginAttempts(0)
                .build();

        // Persist user
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return savedUser;
    }

    /**
     * Find user by ID.
     *
     * @param userId the user ID
     * @return the user
     * @throws UserException if user not found
     */
    public User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
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
        log.debug("Finding user by email: {}", dto.getEmail());

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
    }

    /**
     * Hash password using Base64 encoding.
     * TODO: Replace with BCrypt/Argon2 in production for proper security.
     *
     * @param plainPassword the plain text password
     * @return the hashed password
     */
    private String hashPassword(String plainPassword) {
        // TEMPORARY: For MVP/testing purposes only
        // PRODUCTION: Use BCryptPasswordEncoder or Argon2
        return Base64.getEncoder().encodeToString(plainPassword.getBytes());
    }
}
