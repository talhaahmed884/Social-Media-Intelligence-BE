package com.media.intelligence.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.intelligence.common.validation.core.ValidationError;
import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.user.dto.ChangePasswordDTO;
import com.media.intelligence.user.dto.FindUserByEmailDTO;
import com.media.intelligence.user.dto.RegisterUserDTO;
import com.media.intelligence.user.dto.UpdateUserProfileDTO;
import com.media.intelligence.user.entity.User;
import com.media.intelligence.user.exception.UserErrorCode;
import com.media.intelligence.user.exception.UserException;
import com.media.intelligence.user.sanitization.ChangePasswordDTOSanitizer;
import com.media.intelligence.user.service.UserService;
import com.media.intelligence.user.validation.ChangePasswordDTOValidator;
import com.media.intelligence.user_credential.exception.UserCredentialErrorCode;
import com.media.intelligence.user_credential.exception.UserCredentialException;
import com.media.intelligence.user_credential.service.UserCredentialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("UserController Integration Tests")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserCredentialService userCredentialService;

    @MockitoBean
    private ChangePasswordDTOValidator changePasswordValidator;

    @MockitoBean
    private ChangePasswordDTOSanitizer changePasswordSanitizer;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .fullName("Test User")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
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

        when(userService.registerUser(any(RegisterUserDTO.class))).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Test User"));

        verify(userService).registerUser(any(RegisterUserDTO.class));
    }

    @Test
    @DisplayName("POST /register: Should return 409 when email already exists")
    void registerUser_ShouldReturn409WhenEmailExists() throws Exception {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("existing@example.com")
                .password("SecurePass123!")
                .fullName("User")
                .build();

        when(userService.registerUser(any(RegisterUserDTO.class)))
                .thenThrow(new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS));

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("USR_003"));
    }

    @Test
    @DisplayName("POST /register: Should return 400 when validation fails")
    void registerUser_ShouldReturn400WhenValidationFails() throws Exception {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("invalid")
                .password("weak")
                .fullName("A")
                .build();

        when(userService.registerUser(any(RegisterUserDTO.class)))
                .thenThrow(new UserException(UserErrorCode.INVALID_INPUT));

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
        // Arrange
        when(userService.findUserById(testUserId)).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

        verify(userService).findUserById(testUserId);
    }

    @Test
    @DisplayName("GET /{userId}: Should return 404 when user not found")
    void getUserById_ShouldReturn404WhenUserNotFound() throws Exception {
        // Arrange
        when(userService.findUserById(testUserId))
                .thenThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}", testUserId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("USR_001"));
    }

    // ==================== GET /api/v1/users/by-email Tests ====================

    @Test
    @DisplayName("GET /by-email: Should successfully get user by email")
    void getUserByEmail_ShouldSucceed() throws Exception {
        // Arrange
        when(userService.findUserByEmail(any(FindUserByEmailDTO.class))).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/by-email")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

        verify(userService).findUserByEmail(any(FindUserByEmailDTO.class));
    }

    @Test
    @DisplayName("GET /by-email: Should return 404 when user not found")
    void getUserByEmail_ShouldReturn404WhenUserNotFound() throws Exception {
        // Arrange
        when(userService.findUserByEmail(any(FindUserByEmailDTO.class)))
                .thenThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

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
        // Arrange
        User user1 = User.builder().id(UUID.randomUUID()).email("user1@example.com").fullName("User 1").build();
        User user2 = User.builder().id(UUID.randomUUID()).email("user2@example.com").fullName("User 2").build();
        List<User> users = Arrays.asList(user1, user2);

        when(userService.getAllUsers()).thenReturn(users);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].email").value("user1@example.com"))
                .andExpect(jsonPath("$.data[1].email").value("user2@example.com"));

        verify(userService).getAllUsers();
    }

    @Test
    @DisplayName("GET /: Should return empty list when no users exist")
    void getAllUsers_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(userService.getAllUsers()).thenReturn(List.of());

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
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("updated@example.com")
                .fullName("Updated Name")
                .build();

        User updatedUser = User.builder()
                .id(testUserId)
                .email("updated@example.com")
                .fullName("Updated Name")
                .build();

        when(userService.updateUserProfile(any(UUID.class), any(UpdateUserProfileDTO.class)))
                .thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User profile updated successfully"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"));

        verify(userService).updateUserProfile(any(UUID.class), any(UpdateUserProfileDTO.class));
    }

    @Test
    @DisplayName("PUT /{userId}: Should return 409 when email already exists")
    void updateUserProfile_ShouldReturn409WhenEmailExists() throws Exception {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("existing@example.com")
                .fullName("Name")
                .build();

        when(userService.updateUserProfile(any(UUID.class), any(UpdateUserProfileDTO.class)))
                .thenThrow(new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /{userId}: Should return 404 when user not found")
    void updateUserProfile_ShouldReturn404WhenUserNotFound() throws Exception {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("test@example.com")
                .fullName("Name")
                .build();

        when(userService.updateUserProfile(any(UUID.class), any(UpdateUserProfileDTO.class)))
                .thenThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE /api/v1/users/{userId} Tests ====================

    @Test
    @DisplayName("DELETE /{userId}: Should successfully delete user")
    void deleteUser_ShouldSucceed() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(testUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userService).deleteUser(testUserId);
    }

    @Test
    @DisplayName("DELETE /{userId}: Should return 404 when user not found")
    void deleteUser_ShouldReturn404WhenUserNotFound() throws Exception {
        // Arrange
        doThrow(new UserException(UserErrorCode.USER_NOT_FOUND))
                .when(userService).deleteUser(testUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{userId}", testUserId))
                .andExpect(status().isNotFound());
    }

    // ==================== PUT /api/v1/users/{userId}/change-password Tests ====================

    @Test
    @DisplayName("PUT /{userId}/change-password: Should successfully change password")
    void changePassword_ShouldSucceed() throws Exception {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("NewSecurePass123!")
                .build();

        when(changePasswordSanitizer.sanitize(any())).thenReturn(dto);
        when(changePasswordValidator.validate(any())).thenReturn(ValidationResult.success(any()));
        when(userCredentialService.verifyPassword(any(), dto.getCurrentPassword())).thenReturn(true);
        doNothing().when(userCredentialService).changePassword(any(), any());

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/change-password", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(userCredentialService).changePassword(testUserId, new ChangePasswordDTO("OldPass123!",
                "NewSecurePass123!"));
    }

    @Test
    @DisplayName("PUT /{userId}/change-password: Should return 400 when validation fails")
    void changePassword_ShouldReturn400WhenValidationFails() throws Exception {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("")
                .newPassword("weak")
                .build();

        ValidationResult validationResult = ValidationResult.failure(ValidationError.builder().field("currentPassword")
                .message("Invalid input data").build());

        when(changePasswordSanitizer.sanitize(any())).thenReturn(dto);
        when(changePasswordValidator.validate(any())).thenReturn(validationResult);
        doThrow(new UserException(UserErrorCode.INVALID_INPUT)).when(userCredentialService)
                .changePassword(testUserId, dto);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/change-password", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /{userId}/change-password: Should return 401 when current password is incorrect")
    void changePassword_ShouldReturn401WhenCurrentPasswordIncorrect() throws Exception {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("WrongPassword")
                .newPassword("NewSecurePass123!")
                .build();

        when(changePasswordSanitizer.sanitize(any())).thenReturn(dto);
        when(changePasswordValidator.validate(any())).thenReturn(ValidationResult.success(any()));
        when(userCredentialService.verifyPassword(any(), dto.getCurrentPassword())).thenReturn(false);
        doThrow(new UserCredentialException(UserCredentialErrorCode.CURRENT_PASSWORD_MISMATCH)).doNothing()
                .when(userCredentialService).changePassword(any(), any());

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/change-password", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("CRED_011"));
    }
}
