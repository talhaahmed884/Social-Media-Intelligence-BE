package com.media.intelligence.user_credential.service;

import com.media.intelligence.user.dto.ChangePasswordDTO;
import com.media.intelligence.user.entity.User;
import com.media.intelligence.user.exception.UserErrorCode;
import com.media.intelligence.user.exception.UserException;
import com.media.intelligence.user.repository.UserRepository;
import com.media.intelligence.user_credential.entity.UserCredential;
import com.media.intelligence.user_credential.exception.UserCredentialErrorCode;
import com.media.intelligence.user_credential.exception.UserCredentialException;
import com.media.intelligence.user_credential.repository.UserCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserCredentialService Integration Tests")
public class UserCredentialServiceIntegrationTest {

    @Autowired
    private UserCredentialService credentialService;

    @Autowired
    private UserCredentialRepository credentialRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        credentialRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("test@example.com")
                .fullName("Test User")
                .build();
        testUser = userRepository.save(testUser);
    }

    // ==================== createCredential Tests ====================

    @Test
    @DisplayName("createCredential: Should successfully create credentials")
    void createCredential_ShouldSucceed() {
        // Arrange
        String password = "SecurePass123!";

        // Act
        UserCredential result = credentialService.createCredential(testUser.getId(), password);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(testUser.getId(), result.getUserId());
        assertNotNull(result.getPasswordHash());
        assertNotEquals(password, result.getPasswordHash());

        // Verify in database
        Optional<UserCredential> dbCredential = credentialRepository.findByUser_Id(testUser.getId());
        assertTrue(dbCredential.isPresent());
        assertEquals(result.getId(), dbCredential.get().getId());
    }

    @Test
    @DisplayName("createCredential: Should hash the password")
    void createCredential_ShouldHashPassword() {
        // Arrange
        String password = "SecurePass123!";

        // Act
        UserCredential result = credentialService.createCredential(testUser.getId(), password);

        // Assert
        assertNotEquals(password, result.getPasswordHash());
        assertTrue(result.getPasswordHash().length() > password.length());
    }

    @Test
    @DisplayName("createCredential: Should throw exception when user ID is null")
    void createCredential_ShouldFailWhenUserIdIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.createCredential(null, "password123!")
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_CREATION_FAILED, exception.getErrorCode());
    }

    @Test
    @DisplayName("createCredential: Should throw exception when password is null")
    void createCredential_ShouldFailWhenPasswordIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.createCredential(testUser.getId(), null)
        );

        assertEquals(UserCredentialErrorCode.PASSWORD_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("createCredential: Should throw exception when password is empty")
    void createCredential_ShouldFailWhenPasswordIsEmpty() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.createCredential(testUser.getId(), "   ")
        );

        assertEquals(UserCredentialErrorCode.PASSWORD_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("createCredential: Should throw exception when credentials already exist")
    void createCredential_ShouldFailWhenCredentialsExist() {
        // Arrange
        credentialService.createCredential(testUser.getId(), "FirstPass123!");

        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.createCredential(testUser.getId(), "SecondPass123!")
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_CREATION_FAILED, exception.getErrorCode());
    }

    @Test
    @DisplayName("createCredential: Should throw exception when user ID does not exist")
    void createCredential_ShouldFailWhenUserIdNotFound() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();

        // Act & Assert
        assertThrows(Exception.class, () ->
                credentialService.createCredential(nonExistentUserId, "SecurePass123!")
        );
    }

    // ==================== verifyPassword Tests ====================

    @Test
    @DisplayName("verifyPassword: Should return true for correct password")
    void verifyPassword_ShouldReturnTrueForCorrectPassword() {
        // Arrange
        String password = "SecurePass123!";
        credentialService.createCredential(testUser.getId(), password);

        // Act
        boolean result = credentialService.verifyPassword(testUser.getId(), password);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("verifyPassword: Should return false for incorrect password")
    void verifyPassword_ShouldReturnFalseForIncorrectPassword() {
        // Arrange
        credentialService.createCredential(testUser.getId(), "CorrectPass123!");

        // Act
        boolean result = credentialService.verifyPassword(testUser.getId(), "WrongPass123!");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("verifyPassword: Should throw exception when user ID is null")
    void verifyPassword_ShouldFailWhenUserIdIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.verifyPassword(null, "password")
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("verifyPassword: Should throw exception when password is null")
    void verifyPassword_ShouldFailWhenPasswordIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.verifyPassword(testUser.getId(), null)
        );

        assertEquals(UserCredentialErrorCode.INVALID_PASSWORD, exception.getErrorCode());
    }

    @Test
    @DisplayName("verifyPassword: Should throw exception when credentials not found")
    void verifyPassword_ShouldFailWhenCredentialsNotFound() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.verifyPassword(testUser.getId(), "password")
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("verifyPassword: Should be case-sensitive")
    void verifyPassword_ShouldBeCaseSensitive() {
        // Arrange
        String password = "SecurePass123!";
        credentialService.createCredential(testUser.getId(), password);

        // Act
        boolean result = credentialService.verifyPassword(testUser.getId(), "securepass123!");

        // Assert
        assertFalse(result);
    }

    // ==================== updatePassword Tests ====================

    @Test
    @DisplayName("updatePassword: Should successfully update password")
    void updatePassword_ShouldSucceed() {
        // Arrange
        String oldPassword = "OldPass123!";
        String newPassword = "NewPass456!";
        credentialService.createCredential(testUser.getId(), oldPassword);

        // Act
        credentialService.updatePassword(testUser.getId(), newPassword);

        // Assert
        assertTrue(credentialService.verifyPassword(testUser.getId(), newPassword));
        assertFalse(credentialService.verifyPassword(testUser.getId(), oldPassword));
    }

    @Test
    @DisplayName("updatePassword: Should generate new hash for new password")
    void updatePassword_ShouldGenerateNewHash() {
        // Arrange
        String oldPassword = "OldPass123!";
        String newPassword = "NewPass456!";
        UserCredential credential = credentialService.createCredential(testUser.getId(), oldPassword);
        String oldHash = credential.getPasswordHash();

        // Act
        credentialService.updatePassword(testUser.getId(), newPassword);

        // Assert
        UserCredential updatedCredential = credentialService.getCredentialByUserId(testUser.getId());
        assertNotEquals(oldHash, updatedCredential.getPasswordHash());
    }

    @Test
    @DisplayName("updatePassword: Should throw exception when user ID is null")
    void updatePassword_ShouldFailWhenUserIdIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.updatePassword(null, "NewPass123!")
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_UPDATE_FAILED, exception.getErrorCode());
    }

    @Test
    @DisplayName("updatePassword: Should throw exception when password is null")
    void updatePassword_ShouldFailWhenPasswordIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.updatePassword(testUser.getId(), null)
        );

        assertEquals(UserCredentialErrorCode.PASSWORD_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("updatePassword: Should throw exception when credentials not found")
    void updatePassword_ShouldFailWhenCredentialsNotFound() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.updatePassword(testUser.getId(), "NewPass123!")
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND, exception.getErrorCode());
    }

    // ==================== changePassword Tests ====================

    @Test
    @DisplayName("changePassword: Should successfully change password")
    void changePassword_ShouldSucceed() {
        // Arrange
        String currentPassword = "CurrentPass123!";
        String newPassword = "NewPass456!";
        credentialService.createCredential(testUser.getId(), currentPassword);

        ChangePasswordDTO dto = new ChangePasswordDTO(currentPassword, newPassword);

        // Act
        credentialService.changePassword(testUser.getId(), dto);

        // Assert
        assertTrue(credentialService.verifyPassword(testUser.getId(), newPassword));
        assertFalse(credentialService.verifyPassword(testUser.getId(), currentPassword));
    }

    @Test
    @DisplayName("changePassword: Should throw exception when current password is incorrect")
    void changePassword_ShouldFailWhenCurrentPasswordIncorrect() {
        // Arrange
        String currentPassword = "CorrectPass123!";
        String wrongPassword = "WrongPass123!";
        String newPassword = "NewPass456!";
        credentialService.createCredential(testUser.getId(), currentPassword);

        ChangePasswordDTO dto = new ChangePasswordDTO(wrongPassword, newPassword);

        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.changePassword(testUser.getId(), dto)
        );

        assertEquals(UserCredentialErrorCode.CURRENT_PASSWORD_MISMATCH, exception.getErrorCode());

        // Verify old password still works
        assertTrue(credentialService.verifyPassword(testUser.getId(), currentPassword));
    }

    @Test
    @DisplayName("changePassword: Should throw exception when user ID is null")
    void changePassword_ShouldFailWhenUserIdIsNull() {
        // Arrange
        ChangePasswordDTO dto = new ChangePasswordDTO("Current123!", "New123!");

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                credentialService.changePassword(null, dto)
        );

        assertEquals(UserErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    @DisplayName("changePassword: Should throw exception when current password is null")
    void changePassword_ShouldFailWhenCurrentPasswordIsNull() {
        // Arrange
        credentialService.createCredential(testUser.getId(), "Pass123!");
        ChangePasswordDTO dto = new ChangePasswordDTO(null, "New123!");

        // Act & Assert
        assertThrows(Exception.class, () ->
                credentialService.changePassword(testUser.getId(), dto)
        );
    }

    @Test
    @DisplayName("changePassword: Should throw exception when new password is null")
    void changePassword_ShouldFailWhenNewPasswordIsNull() {
        // Arrange
        credentialService.createCredential(testUser.getId(), "Pass123!");
        ChangePasswordDTO dto = new ChangePasswordDTO("Pass123!", null);

        // Act & Assert
        assertThrows(Exception.class, () ->
                credentialService.changePassword(testUser.getId(), dto)
        );
    }

    @Test
    @DisplayName("changePassword: Should fail with weak new password")
    void changePassword_ShouldFailWithWeakNewPassword() {
        // Arrange
        String currentPassword = "StrongPass123!";
        String weakPassword = "weak";
        credentialService.createCredential(testUser.getId(), currentPassword);

        ChangePasswordDTO dto = new ChangePasswordDTO(currentPassword, weakPassword);

        // Act & Assert
        assertThrows(UserException.class, () ->
                credentialService.changePassword(testUser.getId(), dto)
        );

        // Verify old password still works
        assertTrue(credentialService.verifyPassword(testUser.getId(), currentPassword));
    }

    // ==================== getCredentialByUserId Tests ====================

    @Test
    @DisplayName("getCredentialByUserId: Should successfully retrieve credentials")
    void getCredentialByUserId_ShouldSucceed() {
        // Arrange
        String password = "SecurePass123!";
        UserCredential created = credentialService.createCredential(testUser.getId(), password);

        // Act
        UserCredential result = credentialService.getCredentialByUserId(testUser.getId());

        // Assert
        assertNotNull(result);
        assertEquals(created.getId(), result.getId());
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(created.getPasswordHash(), result.getPasswordHash());
    }

    @Test
    @DisplayName("getCredentialByUserId: Should throw exception when user ID is null")
    void getCredentialByUserId_ShouldFailWhenUserIdIsNull() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.getCredentialByUserId(null)
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("getCredentialByUserId: Should throw exception when credentials not found")
    void getCredentialByUserId_ShouldFailWhenCredentialsNotFound() {
        // Act & Assert
        UserCredentialException exception = assertThrows(UserCredentialException.class, () ->
                credentialService.getCredentialByUserId(testUser.getId())
        );

        assertEquals(UserCredentialErrorCode.CREDENTIAL_NOT_FOUND, exception.getErrorCode());
    }

    // ==================== Multiple Users Tests ====================

    @Test
    @DisplayName("createCredential: Should support credentials for multiple users")
    void createCredential_ShouldSupportMultipleUsers() {
        // Arrange
        User user2 = userRepository.save(User.builder().email("user2@example.com").fullName("User 2").build());
        User user3 = userRepository.save(User.builder().email("user3@example.com").fullName("User 3").build());

        // Act
        UserCredential cred1 = credentialService.createCredential(testUser.getId(), "Pass1!");
        UserCredential cred2 = credentialService.createCredential(user2.getId(), "Pass2!");
        UserCredential cred3 = credentialService.createCredential(user3.getId(), "Pass3!");

        // Assert
        assertNotNull(cred1);
        assertNotNull(cred2);
        assertNotNull(cred3);

        assertTrue(credentialService.verifyPassword(testUser.getId(), "Pass1!"));
        assertTrue(credentialService.verifyPassword(user2.getId(), "Pass2!"));
        assertTrue(credentialService.verifyPassword(user3.getId(), "Pass3!"));

        assertFalse(credentialService.verifyPassword(testUser.getId(), "Pass2!"));
    }

    // ==================== Password Hash Consistency Tests ====================

    @Test
    @DisplayName("createCredential: Should produce different hashes for same password")
    void createCredential_ShouldProduceDifferentHashes() {
        // Arrange
        User user2 = userRepository.save(User.builder().email("user2@example.com").fullName("User 2").build());
        String samePassword = "SamePass123!";

        // Act
        UserCredential cred1 = credentialService.createCredential(testUser.getId(), samePassword);
        UserCredential cred2 = credentialService.createCredential(user2.getId(), samePassword);

        // Assert - Hashes should be different due to salting
        assertNotEquals(cred1.getPasswordHash(), cred2.getPasswordHash());
    }
}
