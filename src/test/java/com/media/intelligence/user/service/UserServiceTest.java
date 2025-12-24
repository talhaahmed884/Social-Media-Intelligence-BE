package com.media.intelligence.user.service;

import com.media.intelligence.common.validation.core.ValidationError;
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
import com.media.intelligence.user_credential.entity.UserCredential;
import com.media.intelligence.user_credential.service.UserCredentialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialService credentialService;

//    @Mock
//    private UserCredentialRepository userCredentialRepository;

    @Mock
    private RegisterUserDTOValidator registerUserValidator;

    @Mock
    private RegisterUserDTOSanitizer registerUserSanitizer;

    @Mock
    private FindUserByEmailDTOValidator findUserByEmailValidator;

    @Mock
    private FindUserByEmailDTOSanitizer findUserByEmailSanitizer;

    @Mock
    private UpdateUserProfileDTOValidator updateUserProfileValidator;

    @Mock
    private UpdateUserProfileDTOSanitizer updateUserProfileSanitizer;

    @InjectMocks
    private UserService userService;

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

    // ==================== registerUser Tests ====================

    @Test
    @DisplayName("registerUser: Should successfully register a new user")
    void registerUser_ShouldSucceed() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("newuser@example.com")
                .password("SecurePass123!")
                .fullName("New User")
                .build();

        when(registerUserSanitizer.sanitize(any())).thenReturn(dto);
        when(registerUserValidator.validate(any())).thenReturn(ValidationResult.success(any()));
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(credentialService.createCredential(any(), anyString())).thenReturn(any(UserCredential.class));

        // Act
        User result = userService.registerUser(dto);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        verify(registerUserSanitizer).sanitize(dto);
        verify(registerUserValidator).validate(dto);
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(userRepository).save(any(User.class));
        verify(credentialService).createCredential(any(UUID.class), eq("SecurePass123!"));
    }

    @Test
    @DisplayName("registerUser: Should throw exception when DTO is null")
    void registerUser_ShouldFailWhenDtoIsNull() {
        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.registerUser(null)
        );

        assertEquals(UserErrorCode.INVALID_INPUT, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser: Should throw exception when validation fails")
    void registerUser_ShouldFailWhenValidationFails() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("invalid")
                .password("weak")
                .fullName("A")
                .build();

        ValidationResult validationResult = ValidationResult.failure(ValidationError.builder().field("email")
                .message("Invalid email format").build());

        when(registerUserSanitizer.sanitize(any())).thenReturn(dto);
        when(registerUserValidator.validate(any())).thenReturn(validationResult);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.registerUser(dto)
        );

        assertEquals(UserErrorCode.INVALID_INPUT, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser: Should throw exception when email already exists")
    void registerUser_ShouldFailWhenEmailExists() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("existing@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();

        when(registerUserSanitizer.sanitize(any())).thenReturn(dto);
        when(registerUserValidator.validate(any())).thenReturn(ValidationResult.success(dto));
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.registerUser(dto)
        );

        assertEquals(UserErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    // ==================== findUserById Tests ====================

    @Test
    @DisplayName("findUserById: Should successfully find user by ID")
    void findUserById_ShouldSucceed() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.findUserById(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getId());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findById(testUserId);
    }

    @Test
    @DisplayName("findUserById: Should throw exception when user ID is null")
    void findUserById_ShouldFailWhenIdIsNull() {
        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.findUserById(null)
        );

        assertEquals(UserErrorCode.USER_ID_INVALID, exception.getErrorCode());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("findUserById: Should throw exception when user not found")
    void findUserById_ShouldFailWhenUserNotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.findUserById(testUserId)
        );

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository).findById(testUserId);
    }

    // ==================== findUserByEmail Tests ====================

    @Test
    @DisplayName("findUserByEmail: Should successfully find user by email")
    void findUserByEmail_ShouldSucceed() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("test@example.com")
                .build();

        when(findUserByEmailSanitizer.sanitize(any())).thenReturn(dto);
        when(findUserByEmailValidator.validate(any())).thenReturn(ValidationResult.success(dto));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.findUserByEmail(dto);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(findUserByEmailSanitizer).sanitize(dto);
        verify(findUserByEmailValidator).validate(dto);
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("findUserByEmail: Should throw exception when DTO is null")
    void findUserByEmail_ShouldFailWhenDtoIsNull() {
        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.findUserByEmail(null)
        );

        assertEquals(UserErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    @DisplayName("findUserByEmail: Should throw exception when validation fails")
    void findUserByEmail_ShouldFailWhenValidationFails() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("invalid-email")
                .build();

        ValidationResult validationResult = ValidationResult.failure(ValidationError.builder().field("email")
                .message("Invalid email format").build());

        when(findUserByEmailSanitizer.sanitize(any())).thenReturn(dto);
        when(findUserByEmailValidator.validate(any())).thenReturn(validationResult);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.findUserByEmail(dto)
        );

        assertEquals(UserErrorCode.INVALID_EMAIL_FORMAT, exception.getErrorCode());
    }

    // ==================== getAllUsers Tests ====================

    @Test
    @DisplayName("getAllUsers: Should return list of all users")
    void getAllUsers_ShouldSucceed() {
        // Arrange
        User user1 = User.builder().id(UUID.randomUUID()).email("user1@example.com").fullName("User 1").build();
        User user2 = User.builder().id(UUID.randomUUID()).email("user2@example.com").fullName("User 2").build();
        List<User> users = Arrays.asList(user1, user2);

        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getAllUsers: Should return empty list when no users exist")
    void getAllUsers_ShouldReturnEmptyList() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of());

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(userRepository).findAll();
    }

    // ==================== updateUserProfile Tests ====================

    @Test
    @DisplayName("updateUserProfile: Should successfully update user profile")
    void updateUserProfile_ShouldSucceed() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("newemail@example.com")
                .fullName("Updated Name")
                .build();

        when(updateUserProfileSanitizer.sanitize(any())).thenReturn(dto);
        when(updateUserProfileValidator.validate(any())).thenReturn(ValidationResult.success(any()));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateUserProfile(testUserId, dto);

        // Assert
        assertNotNull(result);
        verify(updateUserProfileSanitizer).sanitize(dto);
        verify(updateUserProfileValidator).validate(dto);
        verify(userRepository).findById(testUserId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateUserProfile: Should throw exception when user ID is null")
    void updateUserProfile_ShouldFailWhenUserIdIsNull() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("test@example.com")
                .fullName("Test")
                .build();

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.updateUserProfile(null, dto)
        );

        assertEquals(UserErrorCode.USER_ID_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("updateUserProfile: Should throw exception when DTO is null")
    void updateUserProfile_ShouldFailWhenDtoIsNull() {
        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.updateUserProfile(testUserId, null)
        );

        assertEquals(UserErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    @DisplayName("updateUserProfile: Should throw exception when email already exists")
    void updateUserProfile_ShouldFailWhenEmailExists() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("existing@example.com")
                .fullName("Updated Name")
                .build();

        when(updateUserProfileSanitizer.sanitize(any())).thenReturn(dto);
        when(updateUserProfileValidator.validate(any())).thenReturn(ValidationResult.success(any()));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.updateUserProfile(testUserId, dto)
        );

        assertEquals(UserErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserProfile: Should allow keeping same email")
    void updateUserProfile_ShouldAllowKeepingSameEmail() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("test@example.com") // Same as existing
                .fullName("Updated Name")
                .build();

        when(updateUserProfileSanitizer.sanitize(any())).thenReturn(dto);
        when(updateUserProfileValidator.validate(any())).thenReturn(ValidationResult.success(any()));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateUserProfile(testUserId, dto);

        // Assert
        assertNotNull(result);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(any(User.class));
    }

    // ==================== deleteUser Tests ====================

    @Test
    @DisplayName("deleteUser: Should successfully delete user")
    void deleteUser_ShouldSucceed() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
//        doNothing().when(userCredentialRepository).deleteByUserId(any(UUID.class));
        doNothing().when(userRepository).delete(any(User.class));

        // Act
        userService.deleteUser(testUserId);

        // Assert
        verify(userRepository).findById(testUserId);
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteUser: Should throw exception when user ID is null")
    void deleteUser_ShouldFailWhenUserIdIsNull() {
        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.deleteUser(null)
        );

        assertEquals(UserErrorCode.USER_ID_INVALID, exception.getErrorCode());
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteUser: Should throw exception when user not found")
    void deleteUser_ShouldFailWhenUserNotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.deleteUser(testUserId)
        );

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, never()).delete(any());
    }
}
