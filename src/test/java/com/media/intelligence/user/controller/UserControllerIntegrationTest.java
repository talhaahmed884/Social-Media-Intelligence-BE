package com.media.intelligence.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.intelligence.user.dto.ChangePasswordDTO;
import com.media.intelligence.user.dto.RegisterUserDTO;
import com.media.intelligence.user.dto.UpdateUserProfileDTO;
import com.media.intelligence.user.repository.UserRepository;
import com.media.intelligence.user_credential.repository.UserCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserController Integration Tests")
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

    @BeforeEach
    void setUp() {
        credentialRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== POST /api/v1/users/register Tests ====================

    @Test
    @DisplayName("POST /register: Should successfully register a new user")
    void registerUser_ShouldSucceed() throws Exception {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("newuser@example.com")
                .password("SecurePass123!")
                .fullName("New User")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("New User"))
                .andExpect(jsonPath("$.timestamp").exists());

        // Verify in database
        assert userRepository.findByEmail("newuser@example.com").isPresent();
    }

    @Test
    @DisplayName("POST /register: Should sanitize email to lowercase")
    void registerUser_ShouldSanitizeEmail() throws Exception {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("  TEST@EXAMPLE.COM  ")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /register: Should return 409 when email already exists")
    void registerUser_ShouldReturn409WhenEmailExists() throws Exception {
        // Arrange - Create first user
        RegisterUserDTO firstDto = RegisterUserDTO.builder()
                .email("existing@example.com")
                .password("SecurePass123!")
                .fullName("First User")
                .build();
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstDto)));

        // Attempt to register with same email
        RegisterUserDTO secondDto = RegisterUserDTO.builder()
                .email("existing@example.com")
                .password("DifferentPass123!")
                .fullName("Second User")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("USR_003"));
    }

    @Test
    @DisplayName("POST /register: Should return 400 when email is invalid")
    void registerUser_ShouldReturn400WhenEmailInvalid() throws Exception {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("invalid-email")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /register: Should return 400 when password is weak")
    void registerUser_ShouldReturn400WhenPasswordWeak() throws Exception {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("weak")
                .fullName("Test User")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== GET /api/v1/users/{userId} Tests ====================

    @Test
    @DisplayName("GET /{userId}: Should successfully get user by ID")
    void getUserById_ShouldSucceed() throws Exception {
        // Arrange - Register a user first
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();

        String registerResponse = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(registerResponse).get("data").get("id").asText();

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Test User"));
    }

    @Test
    @DisplayName("GET /{userId}: Should return 404 when user not found")
    void getUserById_ShouldReturn404WhenUserNotFound() throws Exception {
        // Arrange
        String nonExistentId = "550e8400-e29b-41d4-a716-446655440000";

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("USR_001"));
    }

    // ==================== GET /api/v1/users/by-email Tests ====================

    @Test
    @DisplayName("GET /by-email: Should successfully get user by email")
    void getUserByEmail_ShouldSucceed() throws Exception {
        // Arrange - Register a user first
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/by-email")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Test User"));
    }

    @Test
    @DisplayName("GET /by-email: Should return 404 when user not found")
    void getUserByEmail_ShouldReturn404WhenUserNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/by-email")
                        .param("email", "nonexistent@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== GET /api/v1/users Tests ====================

    @Test
    @DisplayName("GET /: Should successfully get all users")
    void getAllUsers_ShouldSucceed() throws Exception {
        // Arrange - Register multiple users
        RegisterUserDTO dto1 = RegisterUserDTO.builder()
                .email("user1@example.com")
                .password("Pass123!")
                .fullName("User 1")
                .build();
        RegisterUserDTO dto2 = RegisterUserDTO.builder()
                .email("user2@example.com")
                .password("Pass123!")
                .fullName("User 2")
                .build();

        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto1)));
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto2)));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("GET /: Should return empty list when no users exist")
    void getAllUsers_ShouldReturnEmptyList() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ==================== PUT /api/v1/users/{userId} Tests ====================

    @Test
    @DisplayName("PUT /{userId}: Should successfully update user profile")
    void updateUserProfile_ShouldSucceed() throws Exception {
        // Arrange - Register a user first
        RegisterUserDTO registerDto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();

        String registerResponse = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(registerResponse).get("data").get("id").asText();

        UpdateUserProfileDTO updateDto = UpdateUserProfileDTO.builder()
                .email("updated@example.com")
                .fullName("Updated Name")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User profile updated successfully"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"));

        // Verify in database
        assert userRepository.findByEmail("updated@example.com").isPresent();
        assert userRepository.findByEmail("test@example.com").isEmpty();
    }

    @Test
    @DisplayName("PUT /{userId}: Should return 409 when email already exists")
    void updateUserProfile_ShouldReturn409WhenEmailExists() throws Exception {
        // Arrange - Register two users
        RegisterUserDTO dto1 = RegisterUserDTO.builder()
                .email("user1@example.com")
                .password("Pass123!")
                .fullName("User 1")
                .build();
        RegisterUserDTO dto2 = RegisterUserDTO.builder()
                .email("user2@example.com")
                .password("Pass123!")
                .fullName("User 2")
                .build();

        String response1 = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto2)));

        String user1Id = objectMapper.readTree(response1).get("data").get("id").asText();

        // Try to update user1's email to user2's email
        UpdateUserProfileDTO updateDto = UpdateUserProfileDTO.builder()
                .email("user2@example.com")
                .fullName("User 1 Updated")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== DELETE /api/v1/users/{userId} Tests ====================

    @Test
    @DisplayName("DELETE /{userId}: Should successfully delete user")
    void deleteUser_ShouldSucceed() throws Exception {
        // Arrange - Register a user first
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();

        String registerResponse = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(registerResponse).get("data").get("id").asText();

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        // Verify user is deleted
        assert userRepository.findByEmail("test@example.com").isEmpty();
        assert credentialRepository.findByUser_Id(UUID.fromString(userId)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /{userId}: Should return 404 when user not found")
    void deleteUser_ShouldReturn404WhenUserNotFound() throws Exception {
        // Arrange
        String nonExistentId = "550e8400-e29b-41d4-a716-446655440000";

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{userId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== PUT /api/v1/users/{userId}/change-password Tests ====================

    @Test
    @DisplayName("PUT /{userId}/change-password: Should successfully change password")
    void changePassword_ShouldSucceed() throws Exception {
        // Arrange - Register a user first
        RegisterUserDTO registerDto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("OldPass123!")
                .fullName("Test User")
                .build();

        String registerResponse = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(registerResponse).get("data").get("id").asText();

        ChangePasswordDTO changeDto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("NewSecurePass123!")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/change-password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("PUT /{userId}/change-password: Should return 401 when current password is incorrect")
    void changePassword_ShouldReturn401WhenCurrentPasswordIncorrect() throws Exception {
        // Arrange - Register a user first
        RegisterUserDTO registerDto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("CorrectPass123!")
                .fullName("Test User")
                .build();

        String registerResponse = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(registerResponse).get("data").get("id").asText();

        ChangePasswordDTO changeDto = ChangePasswordDTO.builder()
                .currentPassword("WrongPass123!")
                .newPassword("NewSecurePass123!")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/change-password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("CRED_011"));
    }

    @Test
    @DisplayName("PUT /{userId}/change-password: Should return 400 when new password is weak")
    void changePassword_ShouldReturn400WhenNewPasswordWeak() throws Exception {
        // Arrange - Register a user first
        RegisterUserDTO registerDto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("StrongPass123!")
                .fullName("Test User")
                .build();

        String registerResponse = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(registerResponse).get("data").get("id").asText();

        ChangePasswordDTO changeDto = ChangePasswordDTO.builder()
                .currentPassword("StrongPass123!")
                .newPassword("weak")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/change-password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== End-to-End Flow Tests ====================

    @Test
    @DisplayName("E2E: Complete user lifecycle - register, update, change password, delete")
    void endToEnd_CompleteUserLifecycle() throws Exception {
        // 1. Register user
        RegisterUserDTO registerDto = RegisterUserDTO.builder()
                .email("lifecycle@example.com")
                .password("InitialPass123!")
                .fullName("Lifecycle User")
                .build();

        String registerResponse = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(registerResponse).get("data").get("id").asText();

        // 2. Update profile
        UpdateUserProfileDTO updateDto = UpdateUserProfileDTO.builder()
                .email("updated-lifecycle@example.com")
                .fullName("Updated Lifecycle User")
                .build();

        mockMvc.perform(put("/api/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        // 3. Change password
        ChangePasswordDTO changeDto = ChangePasswordDTO.builder()
                .currentPassword("InitialPass123!")
                .newPassword("UpdatedPass456!")
                .build();

        mockMvc.perform(put("/api/v1/users/{userId}/change-password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeDto)))
                .andExpect(status().isOk());

        // 4. Verify updated user
        mockMvc.perform(get("/api/v1/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("updated-lifecycle@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Updated Lifecycle User"));

        // 5. Delete user
        mockMvc.perform(delete("/api/v1/users/{userId}", userId))
                .andExpect(status().isOk());

        // 6. Verify user is deleted
        mockMvc.perform(get("/api/v1/users/{userId}", userId))
                .andExpect(status().isNotFound());
    }
}
