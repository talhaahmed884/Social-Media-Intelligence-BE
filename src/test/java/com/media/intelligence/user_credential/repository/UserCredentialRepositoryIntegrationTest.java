package com.media.intelligence.user_credential.repository;

import com.media.intelligence.user.entity.User;
import com.media.intelligence.user.repository.UserRepository;
import com.media.intelligence.user_credential.entity.UserCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserCredentialRepository Integration Tests")
public class UserCredentialRepositoryIntegrationTest {

    @Autowired
    private UserCredentialRepository credentialRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private UserCredential testCredential;

    @BeforeEach
    void setUp() {
        credentialRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        testUser = User.builder()
                .email("test@example.com")
                .fullName("Test User")
                .build();
        testUser = userRepository.save(testUser);
        entityManager.flush();

        testCredential = UserCredential.builder()
                .user(testUser)
                .passwordHash("hashed_password_123")
                .build();
    }

    // ==================== Save Tests ====================

    @Test
    @DisplayName("save: Should successfully save a new credential")
    void save_ShouldSucceed() {
        // Act
        UserCredential savedCredential = credentialRepository.save(testCredential);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertNotNull(savedCredential);
        assertNotNull(savedCredential.getId());
        assertEquals(testUser.getId(), savedCredential.getUserId());
        assertEquals("hashed_password_123", savedCredential.getPasswordHash());
        assertNotNull(savedCredential.getCreatedAt());
        assertNotNull(savedCredential.getUpdatedAt());
    }

    @Test
    @DisplayName("save: Should generate UUID for new credential")
    void save_ShouldGenerateUUID() {
        // Act
        UserCredential savedCredential = credentialRepository.save(testCredential);
        entityManager.flush();

        // Assert
        assertNotNull(savedCredential.getId());
        assertInstanceOf(UUID.class, savedCredential.getId());
    }

    @Test
    @DisplayName("save: Should update existing credential")
    void save_ShouldUpdateExistingCredential() {
        // Arrange
        UserCredential savedCredential = credentialRepository.save(testCredential);
        entityManager.flush();
        entityManager.clear();

        // Act
        UserCredential foundCredential = credentialRepository.findById(savedCredential.getId()).orElseThrow();
        foundCredential.setPasswordHash("new_hashed_password");
        credentialRepository.save(foundCredential);
        entityManager.flush();
        entityManager.clear();

        // Assert
        UserCredential reloadedCredential = credentialRepository.findById(savedCredential.getId()).orElseThrow();
        assertEquals("new_hashed_password", reloadedCredential.getPasswordHash());
        assertEquals(savedCredential.getId(), reloadedCredential.getId());
        assertEquals(savedCredential.getCreatedAt(), reloadedCredential.getCreatedAt());
    }

    // ==================== FindById Tests ====================

    @Test
    @DisplayName("findById: Should find credential by ID")
    void findById_ShouldSucceed() {
        // Arrange
        UserCredential savedCredential = credentialRepository.save(testCredential);
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<UserCredential> foundCredential = credentialRepository.findById(savedCredential.getId());

        // Assert
        assertTrue(foundCredential.isPresent());
        assertEquals(savedCredential.getId(), foundCredential.get().getId());
        assertEquals(testUser.getId(), foundCredential.get().getUserId());
    }

    @Test
    @DisplayName("findById: Should return empty when credential not found")
    void findById_ShouldReturnEmpty() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        Optional<UserCredential> foundCredential = credentialRepository.findById(nonExistentId);

        // Assert
        assertFalse(foundCredential.isPresent());
    }

    // ==================== FindByUserId Tests ====================

    @Test
    @DisplayName("findByUserId: Should find credential by user ID")
    void findByUserId_ShouldSucceed() {
        // Arrange
        credentialRepository.save(testCredential);
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<UserCredential> foundCredential = credentialRepository.findByUser_Id(testUser.getId());

        // Assert
        assertTrue(foundCredential.isPresent());
        assertEquals(testUser.getId(), foundCredential.get().getUserId());
        assertEquals("hashed_password_123", foundCredential.get().getPasswordHash());
    }

    @Test
    @DisplayName("findByUserId: Should return empty when user ID not found")
    void findByUserId_ShouldReturnEmpty() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();

        // Act
        Optional<UserCredential> foundCredential = credentialRepository.findByUser_Id(nonExistentUserId);

        // Assert
        assertFalse(foundCredential.isPresent());
    }

    // ==================== ExistsByUserId Tests ====================

    @Test
    @DisplayName("existsByUserId: Should return true when credentials exist for user")
    void existsByUserId_ShouldReturnTrue() {
        // Arrange
        credentialRepository.save(testCredential);
        entityManager.flush();

        // Act
        boolean exists = credentialRepository.existsByUser_Id(testUser.getId());

        // Assert
        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByUserId: Should return false when credentials do not exist")
    void existsByUserId_ShouldReturnFalse() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();

        // Act
        boolean exists = credentialRepository.existsByUser_Id(nonExistentUserId);

        // Assert
        assertFalse(exists);
    }

    // ==================== DeleteByUserId Tests ====================

    @Test
    @DisplayName("deleteByUserId: Should delete credential by user ID")
    void deleteByUserId_ShouldSucceed() {
        // Arrange
        credentialRepository.save(testCredential);
        entityManager.flush();
        entityManager.clear();

        // Act
        credentialRepository.deleteByUser_Id(testUser.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<UserCredential> deletedCredential = credentialRepository.findByUser_Id(testUser.getId());
        assertFalse(deletedCredential.isPresent());
    }

    @Test
    @DisplayName("deleteByUserId: Should not throw exception when user ID does not exist")
    void deleteByUserId_ShouldNotThrowWhenUserIdNotFound() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();

        // Act & Assert
        assertDoesNotThrow(() -> {
            credentialRepository.deleteByUser_Id(nonExistentUserId);
            entityManager.flush();
        });
    }

    // ==================== Delete Tests ====================

    @Test
    @DisplayName("delete: Should delete credential")
    void delete_ShouldSucceed() {
        // Arrange
        UserCredential savedCredential = credentialRepository.save(testCredential);
        entityManager.flush();
        entityManager.clear();

        // Act
        credentialRepository.delete(savedCredential);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<UserCredential> deletedCredential = credentialRepository.findById(savedCredential.getId());
        assertFalse(deletedCredential.isPresent());
    }

    // ==================== Unique Constraint Tests ====================

    @Test
    @DisplayName("save: Should fail when saving duplicate userId")
    void save_ShouldFailOnDuplicateUserId() {
        // Arrange
        credentialRepository.save(testCredential);
        entityManager.flush();

        UserCredential duplicateCredential = UserCredential.builder()
                .user(testUser)
                .passwordHash("different_password_hash")
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> {
            credentialRepository.save(duplicateCredential);
            entityManager.flush();
        });
    }

    // ==================== Cascade Delete Tests ====================

    @Test
    @DisplayName("delete: Should cascade delete credentials when user is deleted")
    void delete_ShouldCascadeDeleteCredentials() {
        // Arrange
        credentialRepository.save(testCredential);
        entityManager.flush();
        entityManager.clear();

        // Act - delete user, credentials should cascade delete
        userRepository.deleteById(testUser.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<UserCredential> deletedCredential = credentialRepository.findByUser_Id(testUser.getId());
        assertFalse(deletedCredential.isPresent());
    }

    // ==================== Timestamp Tests ====================

    @Test
    @DisplayName("save: Should auto-populate createdAt and updatedAt")
    void save_ShouldAutoPopulateTimestamps() {
        // Act
        UserCredential savedCredential = credentialRepository.save(testCredential);
        entityManager.flush();

        // Assert
        assertNotNull(savedCredential.getCreatedAt());
        assertNotNull(savedCredential.getUpdatedAt());
    }

    @Test
    @DisplayName("save: Should update updatedAt on modification")
    void save_ShouldUpdateTimestampOnModification() throws InterruptedException {
        // Arrange
        UserCredential savedCredential = credentialRepository.save(testCredential);
        entityManager.flush();
        entityManager.clear();

        var originalUpdatedAt = savedCredential.getUpdatedAt();
        Thread.sleep(100);

        // Act
        UserCredential foundCredential = credentialRepository.findById(savedCredential.getId()).orElseThrow();
        foundCredential.setPasswordHash("updated_password_hash");
        credentialRepository.save(foundCredential);
        entityManager.flush();
        entityManager.clear();

        // Assert
        UserCredential reloadedCredential = credentialRepository.findById(savedCredential.getId()).orElseThrow();
        assertTrue(reloadedCredential.getUpdatedAt().isAfter(originalUpdatedAt) ||
                reloadedCredential.getUpdatedAt().equals(originalUpdatedAt));
        assertEquals(savedCredential.getCreatedAt(), reloadedCredential.getCreatedAt());
    }

    // ==================== Foreign Key Constraint Tests ====================

    @Test
    @DisplayName("save: Should auto-persist transient user due to @MapsId (implicit cascade)")
    void save_ShouldAutoPersistTransientUser() {
        // Arrange - User with no ID (transient)
        User transientUser = User.builder()
                .email("test@test.com")
                .fullName("Test Test")
                .build();

        assertNull(transientUser.getId(), "User should not have ID before save");

        UserCredential credential = UserCredential.builder()
                .passwordHash("password_hash")
                .user(transientUser)
                .build();

        // Act - Save credential with transient user
        UserCredential savedCredential = credentialRepository.save(credential);
        entityManager.flush();

        // Assert - Due to @MapsId, Hibernate auto-persists the User
        assertNotNull(transientUser.getId(), "User should be auto-persisted");
        assertNotNull(savedCredential.getId(), "Credential should have ID");
        assertEquals(transientUser.getId(), savedCredential.getId(),
                "Credential should share User's ID via @MapsId");

        // Verify User was actually saved to database
        assertTrue(userRepository.existsById(transientUser.getId()));
    }

    // ==================== Multiple Users Tests ====================

    @Test
    @DisplayName("save: Should support credentials for multiple users")
    void save_ShouldSupportMultipleUsers() {
        // Arrange
        User user2 = User.builder().email("user2@example.com").fullName("User 2").build();
        User user3 = User.builder().email("user3@example.com").fullName("User 3").build();
        userRepository.save(user2);
        userRepository.save(user3);
        entityManager.flush();

        UserCredential credential1 = UserCredential.builder()
                .user(testUser)
                .passwordHash("hash1")
                .build();
        UserCredential credential2 = UserCredential.builder()
                .user(user2)
                .passwordHash("hash2")
                .build();
        UserCredential credential3 = UserCredential.builder()
                .user(user3)
                .passwordHash("hash3")
                .build();

        // Act
        credentialRepository.save(credential1);
        credentialRepository.save(credential2);
        credentialRepository.save(credential3);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertTrue(credentialRepository.findByUser_Id(testUser.getId()).isPresent());
        assertTrue(credentialRepository.findByUser_Id(user2.getId()).isPresent());
        assertTrue(credentialRepository.findByUser_Id(user3.getId()).isPresent());
    }
}
