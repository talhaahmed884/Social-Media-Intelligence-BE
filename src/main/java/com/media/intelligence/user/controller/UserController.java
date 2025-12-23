package com.media.intelligence.user.controller;

import com.media.intelligence.common.dto.api_response.ApiResponse;
import com.media.intelligence.user.dto.*;
import com.media.intelligence.user.entity.User;
import com.media.intelligence.user.sanitization.ChangePasswordDTOSanitizer;
import com.media.intelligence.user.service.UserService;
import com.media.intelligence.user.validation.ChangePasswordDTOValidator;
import com.media.intelligence.user_credential.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for User Management endpoints.
 * Isolated to the User Domain - follows vertical slice architecture.
 * <p>
 * Validation and sanitization are handled in the service layer.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserCredentialService userCredentialService;
    private final ChangePasswordDTOValidator changePasswordValidator;
    private final ChangePasswordDTOSanitizer changePasswordSanitizer;

    /**
     * Register a new user.
     *
     * @param dto registration data (validated and sanitized in service layer)
     * @return ApiResponse with created user data
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> registerUser(
            @RequestBody RegisterUserDTO dto) {

        log.info("POST /api/v1/users/register - Email: {}", dto.getEmail());

        User user = userService.registerUser(dto);
        UserResponseDTO response = UserResponseDTO.fromEntity(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    /**
     * Get user by ID.
     *
     * @param userId the user ID
     * @return ApiResponse with user data
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(
            @PathVariable UUID userId) {

        log.info("GET /api/v1/users/{}", userId);

        User user = userService.findUserById(userId);
        UserResponseDTO response = UserResponseDTO.fromEntity(user);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get user by email.
     *
     * @param dto find user by email data (validated and sanitized in service layer)
     * @return ApiResponse with user data
     */
    @GetMapping("/by-email")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserByEmail(
            @ModelAttribute FindUserByEmailDTO dto) {

        log.info("GET /api/v1/users/by-email?email={}", dto.getEmail());

        User user = userService.findUserByEmail(dto);
        UserResponseDTO response = UserResponseDTO.fromEntity(user);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all users.
     *
     * @return ApiResponse with list of all users
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        log.info("GET /api/v1/users - Fetching all users");

        List<User> users = userService.getAllUsers();
        List<UserResponseDTO> response = users.stream()
                .map(UserResponseDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update user profile.
     *
     * @param userId the user ID
     * @param dto    update profile data (validated and sanitized in service layer)
     * @return ApiResponse with updated user data
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserProfile(
            @PathVariable UUID userId,
            @RequestBody UpdateUserProfileDTO dto) {

        log.info("PUT /api/v1/users/{} - Updating profile", userId);

        User user = userService.updateUserProfile(userId, dto);
        UserResponseDTO response = UserResponseDTO.fromEntity(user);

        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", response));
    }

    /**
     * Delete user.
     *
     * @param userId the user ID
     * @return ApiResponse with success message
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId) {
        log.info("DELETE /api/v1/users/{} - Deleting user", userId);

        userService.deleteUser(userId);

        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    /**
     * Change password.
     *
     * @param userId the user ID
     * @param dto    change password data (validated and sanitized in controller)
     * @return ApiResponse with success message
     */
    @PutMapping("/{userId}/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable UUID userId,
            @RequestBody ChangePasswordDTO dto) {

        log.info("PUT /api/v1/users/{}/change-password - Changing password", userId);

        // Sanitize and validate in controller for password change
        changePasswordSanitizer.sanitize(dto);
        var validationResult = changePasswordValidator.validate(dto);

        if (validationResult.hasErrors()) {
            log.warn("Password change validation failed: {}", validationResult.getDetailedErrorMessage());
            throw new com.media.intelligence.user.exception.UserException(
                    com.media.intelligence.user.exception.UserErrorCode.INVALID_INPUT);
        }

        userCredentialService.changePassword(userId, dto.getCurrentPassword(), dto.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
