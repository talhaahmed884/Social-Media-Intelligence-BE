package com.media.intelligence.user.controller;

import com.media.intelligence.common.dto.ApiResponse;
import com.media.intelligence.user.dto.FindUserByEmailDTO;
import com.media.intelligence.user.dto.RegisterUserDTO;
import com.media.intelligence.user.dto.UserResponseDTO;
import com.media.intelligence.user.entity.User;
import com.media.intelligence.user.service.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
    private final UserRegistrationService userRegistrationService;

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

        User user = userRegistrationService.registerUser(dto);
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

        User user = userRegistrationService.findUserById(userId);
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

        User user = userRegistrationService.findUserByEmail(dto);
        UserResponseDTO response = UserResponseDTO.fromEntity(user);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
