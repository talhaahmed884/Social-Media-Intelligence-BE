package com.media.intelligence.user.service;

import com.media.intelligence.user.dto.FindUserByEmailDTO;
import com.media.intelligence.user.dto.RegisterUserDTO;
import com.media.intelligence.user.dto.UpdateUserProfileDTO;
import com.media.intelligence.user.entity.User;
import com.media.intelligence.user.exception.UserErrorCode;
import com.media.intelligence.user.exception.UserException;
import com.media.intelligence.user.repository.UserRepository;
import com.media.intelligence.user_credential.entity.UserCredential;
import com.media.intelligence.user_credential.repository.UserCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserService Integration Tests")
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

    @BeforeEach
    void setUp() {
        credentialRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== registerUser Tests ====================

    @Test
    @DisplayName("registerUser: Should successfully register a new user with valid data")
    void registerUser_ShouldSucceed() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("newuser@example.com")
                .password("SecurePass123!")
                .fullName("New User")
                .build();

        // Act
        User result = userService.registerUser(dto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("newuser@example.com", result.getEmail());
        assertEquals("New User", result.getFullName());

        // Verify user is saved in database
        Optional<User> savedUser = userRepository.findById(result.getId());
        assertTrue(savedUser.isPresent());
        assertEquals("newuser@example.com", savedUser.get().getEmail());

        // Verify credentials are created
        Optional<UserCredential> credential = credentialRepository.findByUser_Id(result.getId());
        assertTrue(credential.isPresent());
        assertNotNull(credential.get().getPasswordHash());
    }

    @Test
    @DisplayName("registerUser: Should sanitize and normalize email")
    void registerUser_ShouldSanitizeEmail() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("  TEST@EXAMPLE.COM  ")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();

        // Act
        User result = userService.registerUser(dto);

        // Assert
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    @DisplayName("registerUser: Should sanitize fullName")
    void registerUser_ShouldSanitizeFullName() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("  Test   User  ")
                .build();

        // Act
        User result = userService.registerUser(dto);

        // Assert
        assertEquals("Test   User", result.getFullName());
    }

    @Test
    @DisplayName("registerUser: Should throw exception when DTO is null")
    void registerUser_ShouldFailWhenDtoIsNull() {
        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.registerUser(null)
        );

        assertEquals(UserErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    @DisplayName("registerUser: Should throw exception when email already exists")
    void registerUser_ShouldFailWhenEmailExists() {
        // Arrange
        RegisterUserDTO firstDto = RegisterUserDTO.builder()
                .email("existing@example.com")
                .password("SecurePass123!")
                .fullName("First User")
                .build();
        userService.registerUser(firstDto);

        RegisterUserDTO secondDto = RegisterUserDTO.builder()
                .email("existing@example.com")
                .password("AnotherPass123!")
                .fullName("Second User")
                .build();

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.registerUser(secondDto)
        );

        assertEquals(UserErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("registerUser: Should fail when email is invalid")
    void registerUser_ShouldFailWhenEmailIsInvalid() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("invalid-email")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();

        // Act & Assert
        assertThrows(UserException.class, () ->
                userService.registerUser(dto)
        );
    }

    @Test
    @DisplayName("registerUser: Should fail when password is weak")
    void registerUser_ShouldFailWhenPasswordIsWeak() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("weak")
                .fullName("Test User")
                .build();

        // Act & Assert
        assertThrows(UserException.class, () ->
                userService.registerUser(dto)
        );
    }

    @Test
    @DisplayName("registerUser: Should fail when fullName is too short")
    void registerUser_ShouldFailWhenFullNameTooShort() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("A")
                .build();

        // Act & Assert
        assertThrows(UserException.class, () ->
                userService.registerUser(dto)
        );
    }

    // ==================== findUserById Tests ====================

    @Test
    @DisplayName("findUserById: Should find user by ID")
    void findUserById_ShouldSucceed() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();
        User savedUser = userService.registerUser(dto);

        // Act
        User foundUser = userService.findUserById(savedUser.getId());

        // Assert
        assertNotNull(foundUser);
        assertEquals(savedUser.getId(), foundUser.getId());
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    @DisplayName("findUserById: Should throw exception when user ID is null")
    void findUserById_ShouldFailWhenIdIsNull() {
        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.findUserById(null)
        );

        assertEquals(UserErrorCode.USER_ID_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("findUserById: Should throw exception when user not found")
    void findUserById_ShouldFailWhenUserNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.findUserById(nonExistentId)
        );

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    // ==================== findUserByEmail Tests ====================

    @Test
    @DisplayName("findUserByEmail: Should find user by email")
    void findUserByEmail_ShouldSucceed() {
        // Arrange
        RegisterUserDTO registerDto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();
        userService.registerUser(registerDto);

        FindUserByEmailDTO findDto = FindUserByEmailDTO.builder()
                .email("test@example.com")
                .build();

        // Act
        User foundUser = userService.findUserByEmail(findDto);

        // Assert
        assertNotNull(foundUser);
        assertEquals("test@example.com", foundUser.getEmail());
        assertEquals("Test User", foundUser.getFullName());
    }

    @Test
    @DisplayName("findUserByEmail: Should sanitize email before search")
    void findUserByEmail_ShouldSanitizeEmail() {
        // Arrange
        RegisterUserDTO registerDto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();
        userService.registerUser(registerDto);

        FindUserByEmailDTO findDto = FindUserByEmailDTO.builder()
                .email("  TEST@EXAMPLE.COM  ")
                .build();

        // Act
        User foundUser = userService.findUserByEmail(findDto);

        // Assert
        assertNotNull(foundUser);
        assertEquals("test@example.com", foundUser.getEmail());
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
    @DisplayName("findUserByEmail: Should throw exception when user not found")
    void findUserByEmail_ShouldFailWhenUserNotFound() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("nonexistent@example.com")
                .build();

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.findUserByEmail(dto)
        );

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("findUserByEmail: Should fail when email is invalid")
    void findUserByEmail_ShouldFailWhenEmailIsInvalid() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("invalid-email")
                .build();

        // Act & Assert
        assertThrows(UserException.class, () ->
                userService.findUserByEmail(dto)
        );
    }

    // ==================== getAllUsers Tests ====================

    @Test
    @DisplayName("getAllUsers: Should return all users")
    void getAllUsers_ShouldSucceed() {
        // Arrange
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
        RegisterUserDTO dto3 = RegisterUserDTO.builder()
                .email("user3@example.com")
                .password("Pass123!")
                .fullName("User 3")
                .build();

        userService.registerUser(dto1);
        userService.registerUser(dto2);
        userService.registerUser(dto3);

        // Act
        List<User> users = userService.getAllUsers();

        // Assert
        assertEquals(3, users.size());
    }

    @Test
    @DisplayName("getAllUsers: Should return empty list when no users exist")
    void getAllUsers_ShouldReturnEmptyList() {
        // Act
        List<User> users = userService.getAllUsers();

        // Assert
        assertTrue(users.isEmpty());
    }

    // ==================== updateUserProfile Tests ====================

    @Test
    @DisplayName("updateUserProfile: Should successfully update user profile")
    void updateUserProfile_ShouldSucceed() {
        // Arrange
        RegisterUserDTO registerDto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();
        User savedUser = userService.registerUser(registerDto);

        UpdateUserProfileDTO updateDto = UpdateUserProfileDTO.builder()
                .email("newemail@example.com")
                .fullName("Updated Name")
                .build();

        // Act
        User updatedUser = userService.updateUserProfile(savedUser.getId(), updateDto);

        // Assert
        assertNotNull(updatedUser);
        assertEquals(savedUser.getId(), updatedUser.getId());
        assertEquals("newemail@example.com", updatedUser.getEmail());
        assertEquals("Updated Name", updatedUser.getFullName());

        // Verify in database
        User dbUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertEquals("newemail@example.com", dbUser.getEmail());
        assertEquals("Updated Name", dbUser.getFullName());
    }

    @Test
    @DisplayName("updateUserProfile: Should allow keeping same email")
    void updateUserProfile_ShouldAllowKeepingSameEmail() {
        // Arrange
        RegisterUserDTO registerDto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();
        User savedUser = userService.registerUser(registerDto);

        UpdateUserProfileDTO updateDto = UpdateUserProfileDTO.builder()
                .email("test@example.com")
                .fullName("Updated Name")
                .build();

        // Act
        User updatedUser = userService.updateUserProfile(savedUser.getId(), updateDto);

        // Assert
        assertEquals("test@example.com", updatedUser.getEmail());
        assertEquals("Updated Name", updatedUser.getFullName());
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
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.updateUserProfile(userId, null)
        );

        assertEquals(UserErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    @DisplayName("updateUserProfile: Should throw exception when email already exists")
    void updateUserProfile_ShouldFailWhenEmailExists() {
        // Arrange
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

        User user1 = userService.registerUser(dto1);
        userService.registerUser(dto2);

        UpdateUserProfileDTO updateDto = UpdateUserProfileDTO.builder()
                .email("user2@example.com")
                .fullName("User 1 Updated")
                .build();

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.updateUserProfile(user1.getId(), updateDto)
        );

        assertEquals(UserErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("updateUserProfile: Should throw exception when user not found")
    void updateUserProfile_ShouldFailWhenUserNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("test@example.com")
                .fullName("Test")
                .build();

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.updateUserProfile(nonExistentId, dto)
        );

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("updateUserProfile: Should sanitize inputs")
    void updateUserProfile_ShouldSanitizeInputs() {
        // Arrange
        RegisterUserDTO registerDto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();
        User savedUser = userService.registerUser(registerDto);

        UpdateUserProfileDTO updateDto = UpdateUserProfileDTO.builder()
                .email("  UPDATED@EXAMPLE.COM  ")
                .fullName("  Updated   Name  ")
                .build();

        // Act
        User updatedUser = userService.updateUserProfile(savedUser.getId(), updateDto);

        // Assert
        assertEquals("updated@example.com", updatedUser.getEmail());
        assertEquals("Updated   Name", updatedUser.getFullName());
    }

    // ==================== deleteUser Tests ====================

    @Test
    @DisplayName("deleteUser: Should successfully delete user and credentials")
    void deleteUser_ShouldSucceed() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .fullName("Test User")
                .build();
        User savedUser = userService.registerUser(dto);
        UUID userId = savedUser.getId();

        // Verify user and credentials exist
        assertTrue(userRepository.findById(userId).isPresent());
        assertTrue(credentialRepository.findByUser_Id(userId).isPresent());

        // Act
        userService.deleteUser(userId);

        // Assert
        assertFalse(userRepository.findById(userId).isPresent());
        assertFalse(credentialRepository.findByUser_Id(userId).isPresent());
    }

    @Test
    @DisplayName("deleteUser: Should throw exception when user ID is null")
    void deleteUser_ShouldFailWhenUserIdIsNull() {
        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.deleteUser(null)
        );

        assertEquals(UserErrorCode.USER_ID_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("deleteUser: Should throw exception when user not found")
    void deleteUser_ShouldFailWhenUserNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                userService.deleteUser(nonExistentId)
        );

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}
