package com.media.intelligence.user_credential.service;

import com.media.intelligence.common.validation.core.ValidationError;
import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.user.dto.ChangePasswordDTO;
import com.media.intelligence.user.exception.UserErrorCode;
import com.media.intelligence.user.exception.UserException;
import com.media.intelligence.user.sanitization.ChangePasswordDTOSanitizer;
import com.media.intelligence.user.validation.ChangePasswordDTOValidator;
import com.media.intelligence.user_credential.entity.UserCredential;
import com.media.intelligence.user_credential.exception.UserCredentialErrorCode;
import com.media.intelligence.user_credential.exception.UserCredentialException;
import com.media.intelligence.user_credential.repository.UserCredentialRepository;
import com.media.intelligence.user_credential.strategy.PasswordHashingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCredentialService Tests")
public class UserCredentialServiceTest {

    @Mock
    private UserCredentialRepository credentialRepository;

    @Mock
    private PasswordHashingStrategy hashingStrategy;

    @Mock
    private ChangePasswordDTOSanitizer changePasswordDTOSanitizer;

    @Mock
    private ChangePasswordDTOValidator changePasswordDTOValidator;

    @InjectMocks
    private UserCredentialService userCredentialService;

    private UUID testUserId;
    private UserCredential testCredential;
    private String testPassword;
    private String testPasswordHash;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPassword = "SecurePass123!";
        testPasswordHash = "hashed_secure_pass_123";

        testCredential = UserCredential.builder()
                .id(UUID.randomUUID())
                .passwordHash(testPasswordHash)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testCredential.setUserId(testUserId);
    }

    // ==================== createCredential Tests ====================

    @Test
    @DisplayName("createCredential: Should successfully create credentials")
    void createCredential_ShouldSucceed() {
        // Arrange
        when(credentialRepository.existsByUser_Id(testUserId)).thenReturn(false);
        when(hashingStrategy.hash(testPassword)).thenReturn(testPasswordHash);
        when(credentialRepository.save(any(UserCredential.class))).thenReturn(testCredential);

        // Act
        UserCredential result = userCredentialService.createCredential(testUserId, testPassword);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        verify(credentialRepository).existsByUser_Id(testUserId);
        verify(hashingStrategy).hash(testPassword);
        verify(credentialRepository).save(any(UserCredential.class));
    }

    @Test
    @DisplayName("createCredential: Should throw exception when user ID is null")
    void createCredential_ShouldFailWhenUserIdIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.createCredential(null, testPassword)
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_CREATION_FAILED, exception.getErrorCode());
        verify(credentialRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCredential: Should throw exception when password is null")
    void createCredential_ShouldFailWhenPasswordIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.createCredential(testUserId, null)
        );

        assertEquals(UserCredentialErrorCode.PASSWORD_REQUIRED, exception.getErrorCode());
        verify(credentialRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCredential: Should throw exception when password is empty")
    void createCredential_ShouldFailWhenPasswordIsEmpty() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.createCredential(testUserId, "   ")
        );

        assertEquals(UserCredentialErrorCode.PASSWORD_REQUIRED, exception.getErrorCode());
        verify(credentialRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCredential: Should throw exception when credentials already exist")
    void createCredential_ShouldFailWhenCredentialsExist() {
        // Arrange
        when(credentialRepository.existsByUser_Id(testUserId)).thenReturn(true);

        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.createCredential(testUserId, testPassword)
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_CREATION_FAILED, exception.getErrorCode());
        verify(credentialRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCredential: Should throw exception when hashing fails")
    void createCredential_ShouldFailWhenHashingFails() {
        // Arrange
        when(credentialRepository.existsByUser_Id(testUserId)).thenReturn(false);
        when(hashingStrategy.hash(testPassword)).thenThrow(new RuntimeException("Hashing error"));

        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.createCredential(testUserId, testPassword)
        );

        assertEquals(UserCredentialErrorCode.PASSWORD_HASH_FAILED, exception.getErrorCode());
        verify(credentialRepository, never()).save(any());
    }

    // ==================== verifyPassword Tests ====================

    @Test
    @DisplayName("verifyPassword: Should return true for correct password")
    void verifyPassword_ShouldReturnTrueForCorrectPassword() {
        // Arrange
        when(credentialRepository.findByUser_Id(testUserId)).thenReturn(Optional.of(testCredential));
        when(hashingStrategy.verify(testPassword, testPasswordHash)).thenReturn(true);

        // Act
        boolean result = userCredentialService.verifyPassword(testUserId, testPassword);

        // Assert
        assertTrue(result);
        verify(credentialRepository).findByUser_Id(testUserId);
        verify(hashingStrategy).verify(testPassword, testPasswordHash);
    }

    @Test
    @DisplayName("verifyPassword: Should return false for incorrect password")
    void verifyPassword_ShouldReturnFalseForIncorrectPassword() {
        // Arrange
        when(credentialRepository.findByUser_Id(testUserId)).thenReturn(Optional.of(testCredential));
        when(hashingStrategy.verify("wrongpassword", testPasswordHash)).thenReturn(false);

        // Act
        boolean result = userCredentialService.verifyPassword(testUserId, "wrongpassword");

        // Assert
        assertFalse(result);
        verify(credentialRepository).findByUser_Id(testUserId);
        verify(hashingStrategy).verify("wrongpassword", testPasswordHash);
    }

    @Test
    @DisplayName("verifyPassword: Should throw exception when user ID is null")
    void verifyPassword_ShouldFailWhenUserIdIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.verifyPassword(null, testPassword)
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("verifyPassword: Should throw exception when password is null")
    void verifyPassword_ShouldFailWhenPasswordIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.verifyPassword(testUserId, null)
        );

        assertEquals(UserCredentialErrorCode.INVALID_PASSWORD, exception.getErrorCode());
    }

    @Test
    @DisplayName("verifyPassword: Should throw exception when credentials not found")
    void verifyPassword_ShouldFailWhenCredentialsNotFound() {
        // Arrange
        when(credentialRepository.findByUser_Id(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.verifyPassword(testUserId, testPassword)
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND, exception.getErrorCode());
    }

    // ==================== updatePassword Tests ====================

    @Test
    @DisplayName("updatePassword: Should successfully update password")
    void updatePassword_ShouldSucceed() {
        // Arrange
        String newPassword = "NewSecurePass123!";
        String newPasswordHash = "hashed_new_secure_pass_123";

        when(credentialRepository.findByUser_Id(testUserId)).thenReturn(Optional.of(testCredential));
        when(hashingStrategy.hash(newPassword)).thenReturn(newPasswordHash);
        when(credentialRepository.save(any(UserCredential.class))).thenReturn(testCredential);

        // Act
        userCredentialService.updatePassword(testUserId, newPassword);

        // Assert
        verify(credentialRepository).findByUser_Id(testUserId);
        verify(hashingStrategy).hash(newPassword);
        verify(credentialRepository).save(testCredential);
        assertEquals(newPasswordHash, testCredential.getPasswordHash());
    }

    @Test
    @DisplayName("updatePassword: Should throw exception when user ID is null")
    void updatePassword_ShouldFailWhenUserIdIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.updatePassword(null, "NewPass123!")
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_UPDATE_FAILED, exception.getErrorCode());
    }

    @Test
    @DisplayName("updatePassword: Should throw exception when password is null")
    void updatePassword_ShouldFailWhenPasswordIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.updatePassword(testUserId, null)
        );

        assertEquals(UserCredentialErrorCode.PASSWORD_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("updatePassword: Should throw exception when credentials not found")
    void updatePassword_ShouldFailWhenCredentialsNotFound() {
        // Arrange
        when(credentialRepository.findByUser_Id(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.updatePassword(testUserId, "NewPass123!")
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND, exception.getErrorCode());
        verify(credentialRepository, never()).save(any());
    }

    // ==================== changePassword Tests ====================

    @Test
    @DisplayName("changePassword: Should successfully change password")
    void changePassword_ShouldSucceed() {
        // Arrange
        String currentPassword = "CurrentPass123!";
        String newPassword = "NewSecurePass123!";
        String newPasswordHash = "hashed_new_secure_pass_123";
        ChangePasswordDTO dto = new ChangePasswordDTO(currentPassword, newPassword);

        when(changePasswordDTOSanitizer.sanitize(any())).thenReturn(dto);
        when(changePasswordDTOValidator.validate(any())).thenReturn(ValidationResult.success(dto));
        when(credentialRepository.findByUser_Id(testUserId)).thenReturn(Optional.of(testCredential));
        when(hashingStrategy.verify(currentPassword, testPasswordHash)).thenReturn(true);
        when(hashingStrategy.hash(newPassword)).thenReturn(newPasswordHash);
        when(credentialRepository.save(any(UserCredential.class))).thenReturn(testCredential);

        // Act
        userCredentialService.changePassword(testUserId, dto);

        // Assert
        verify(credentialRepository, times(2)).findByUser_Id(testUserId);
        verify(hashingStrategy).verify(currentPassword, testPasswordHash);
        verify(hashingStrategy).hash(newPassword);
        verify(credentialRepository).save(any(UserCredential.class));
    }

    @Test
    @DisplayName("changePassword: Should throw exception when current password is incorrect")
    void changePassword_ShouldFailWhenCurrentPasswordIncorrect() {
        // Arrange
        String currentPassword = "WrongPassword";
        String newPassword = "NewSecurePass123!";

        ChangePasswordDTO dto = new ChangePasswordDTO(currentPassword, newPassword);

        when(changePasswordDTOSanitizer.sanitize(any())).thenReturn(dto);
        when(changePasswordDTOValidator.validate(any())).thenReturn(ValidationResult.success(dto));
        when(credentialRepository.findByUser_Id(testUserId)).thenReturn(Optional.of(testCredential));
        when(hashingStrategy.verify(currentPassword, testPasswordHash)).thenReturn(false);

        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.changePassword(testUserId, dto)
        );

        assertEquals(UserCredentialErrorCode.CURRENT_PASSWORD_MISMATCH, exception.getErrorCode());
        verify(credentialRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword: Should throw exception when user ID is null")
    void changePassword_ShouldFailWhenUserIdIsNull() {
        ChangePasswordDTO dto = new ChangePasswordDTO("Current123!", "New123!");

        // Act & Assert
        when(changePasswordDTOSanitizer.sanitize(any())).thenReturn(dto);
        when(changePasswordDTOValidator.validate(any())).thenReturn(ValidationResult.success(dto));
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.changePassword(null, dto)
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_UPDATE_FAILED, exception.getErrorCode());
    }

    @Test
    @DisplayName("changePassword: Should throw exception when current password is null")
    void changePassword_ShouldFailWhenCurrentPasswordIsNull() {
        ChangePasswordDTO dto = new ChangePasswordDTO(null, "New123!");

        // Act & Assert
        when(changePasswordDTOSanitizer.sanitize(any())).thenReturn(dto);
        when(changePasswordDTOValidator.validate(any())).thenReturn(ValidationResult.success(dto));
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.changePassword(testUserId, dto)
        );

        assertEquals(UserCredentialErrorCode.INVALID_PASSWORD, exception.getErrorCode());
    }

    @Test
    @DisplayName("changePassword: Should throw exception when new password is null")
    void changePassword_ShouldFailWhenNewPasswordIsNull() {
        ChangePasswordDTO dto = new ChangePasswordDTO("Current123!", null);

        // Act & Assert
        when(changePasswordDTOSanitizer.sanitize(any())).thenReturn(dto);
        when(changePasswordDTOValidator.validate(any())).thenReturn(ValidationResult.failure(ValidationError.builder().
                code("REQUIRED").message("This field is required").build()));
        UserException exception = assertThrows(UserException.class, () ->
                userCredentialService.changePassword(testUserId, dto)
        );

        assertEquals(UserErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    // ==================== getCredentialByUserId Tests ====================

    @Test
    @DisplayName("getCredentialByUserId: Should successfully retrieve credentials")
    void getCredentialByUserId_ShouldSucceed() {
        // Arrange
        when(credentialRepository.findByUser_Id(testUserId)).thenReturn(Optional.of(testCredential));

        // Act
        UserCredential result = userCredentialService.getCredentialByUserId(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testPasswordHash, result.getPasswordHash());
        verify(credentialRepository).findByUser_Id(testUserId);
    }

    @Test
    @DisplayName("getCredentialByUserId: Should throw exception when user ID is null")
    void getCredentialByUserId_ShouldFailWhenUserIdIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.getCredentialByUserId(null)
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("getCredentialByUserId: Should throw exception when credentials not found")
    void getCredentialByUserId_ShouldFailWhenCredentialsNotFound() {
        // Arrange
        when(credentialRepository.findByUser_Id(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                userCredentialService.getCredentialByUserId(testUserId)
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND, exception.getErrorCode());
    }
}
