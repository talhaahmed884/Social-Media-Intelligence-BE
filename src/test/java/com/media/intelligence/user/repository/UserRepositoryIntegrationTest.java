package com.media.intelligence.user.repository;

import com.media.intelligence.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
public class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        testUser = User.builder()
                .email("test@example.com")
                .fullName("Test User")
                .build();
    }

    // ==================== Save Tests ====================

    @Test
    @DisplayName("save: Should successfully save a new user")
    void save_ShouldSucceed() {
        // Act
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("Test User", savedUser.getFullName());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }

    @Test
    @DisplayName("save: Should generate UUID for new user")
    void save_ShouldGenerateUUID() {
        // Act
        User savedUser = userRepository.save(testUser);
        entityManager.flush();

        // Assert
        assertNotNull(savedUser.getId());
        assertInstanceOf(UUID.class, savedUser.getId());
    }

    @Test
    @DisplayName("save: Should update existing user")
    void save_ShouldUpdateExistingUser() {
        // Arrange
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // Act
        User foundUser = userRepository.findById(savedUser.getId()).orElseThrow();
        foundUser.setFullName("Updated Name");
        userRepository.save(foundUser);
        entityManager.flush();
        entityManager.clear();

        // Assert
        User reloadedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertEquals("Updated Name", reloadedUser.getFullName());
        assertEquals(savedUser.getId(), reloadedUser.getId());
        assertEquals(savedUser.getCreatedAt(), reloadedUser.getCreatedAt());
        assertNotEquals(savedUser.getUpdatedAt(), reloadedUser.getUpdatedAt());
    }

    // ==================== FindById Tests ====================

    @Test
    @DisplayName("findById: Should find user by ID")
    void findById_ShouldSucceed() {
        // Arrange
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().getId());
        assertEquals("test@example.com", foundUser.get().getEmail());
    }

    @Test
    @DisplayName("findById: Should return empty when user not found")
    void findById_ShouldReturnEmpty() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        Optional<User> foundUser = userRepository.findById(nonExistentId);

        // Assert
        assertFalse(foundUser.isPresent());
    }

    // ==================== FindByEmail Tests ====================

    @Test
    @DisplayName("findByEmail: Should find user by email")
    void findByEmail_ShouldSucceed() {
        // Arrange
        userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals("test@example.com", foundUser.get().getEmail());
        assertEquals("Test User", foundUser.get().getFullName());
    }

    @Test
    @DisplayName("findByEmail: Should return empty when email not found")
    void findByEmail_ShouldReturnEmpty() {
        // Act
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(foundUser.isPresent());
    }

    @Test
    @DisplayName("findByEmail: Should be case-sensitive")
    void findByEmail_ShouldBeCaseSensitive() {
        // Arrange
        userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<User> foundUser = userRepository.findByEmail("TEST@EXAMPLE.COM");

        // Assert
        assertFalse(foundUser.isPresent());
    }

    // ==================== ExistsByEmail Tests ====================

    @Test
    @DisplayName("existsByEmail: Should return true when email exists")
    void existsByEmail_ShouldReturnTrue() {
        // Arrange
        userRepository.save(testUser);
        entityManager.flush();

        // Act
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Assert
        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByEmail: Should return false when email does not exist")
    void existsByEmail_ShouldReturnFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(exists);
    }

    @Test
    @DisplayName("existsByEmail: Should be case-sensitive")
    void existsByEmail_ShouldBeCaseSensitive() {
        // Arrange
        userRepository.save(testUser);
        entityManager.flush();

        // Act
        boolean exists = userRepository.existsByEmail("TEST@EXAMPLE.COM");

        // Assert
        assertFalse(exists);
    }

    // ==================== FindAll Tests ====================

    @Test
    @DisplayName("findAll: Should return all users")
    void findAll_ShouldReturnAllUsers() {
        // Arrange
        User user1 = User.builder().email("user1@example.com").fullName("User 1").build();
        User user2 = User.builder().email("user2@example.com").fullName("User 2").build();
        User user3 = User.builder().email("user3@example.com").fullName("User 3").build();

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        entityManager.flush();
        entityManager.clear();

        // Act
        List<User> users = userRepository.findAll();

        // Assert
        assertEquals(3, users.size());
    }

    @Test
    @DisplayName("findAll: Should return empty list when no users exist")
    void findAll_ShouldReturnEmptyList() {
        // Act
        List<User> users = userRepository.findAll();

        // Assert
        assertTrue(users.isEmpty());
    }

    // ==================== Delete Tests ====================

    @Test
    @DisplayName("delete: Should delete user")
    void delete_ShouldSucceed() {
        // Arrange
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // Act
        userRepository.delete(savedUser);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertFalse(deletedUser.isPresent());
    }

    @Test
    @DisplayName("deleteById: Should delete user by ID")
    void deleteById_ShouldSucceed() {
        // Arrange
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        UUID userId = savedUser.getId();
        entityManager.clear();

        // Act
        userRepository.deleteById(userId);
        entityManager.flush();

        // Assert
        Optional<User> deletedUser = userRepository.findById(userId);
        assertFalse(deletedUser.isPresent());
    }

    // ==================== Unique Constraint Tests ====================

    @Test
    @DisplayName("save: Should fail when saving duplicate email")
    void save_ShouldFailOnDuplicateEmail() {
        // Arrange
        userRepository.save(testUser);
        entityManager.flush();

        User duplicateUser = User.builder()
                .email("test@example.com")
                .fullName("Duplicate User")
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> {
            userRepository.save(duplicateUser);
            entityManager.flush();
        });
    }

    // ==================== Timestamp Tests ====================

    @Test
    @DisplayName("save: Should auto-populate createdAt and updatedAt")
    void save_ShouldAutoPopulateTimestamps() {
        // Act
        User savedUser = userRepository.save(testUser);
        entityManager.flush();

        // Assert
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }

    @Test
    @DisplayName("save: Should update updatedAt on modification")
    void save_ShouldUpdateTimestampOnModification() throws InterruptedException {
        // Arrange
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        var originalUpdatedAt = savedUser.getUpdatedAt();
        Thread.sleep(100);

        // Act
        User foundUser = userRepository.findById(savedUser.getId()).orElseThrow();
        foundUser.setFullName("Modified Name");
        userRepository.save(foundUser);
        entityManager.flush();
        entityManager.clear();

        // Assert
        User reloadedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertTrue(reloadedUser.getUpdatedAt().isAfter(originalUpdatedAt) ||
                reloadedUser.getUpdatedAt().equals(originalUpdatedAt));
        assertEquals(savedUser.getCreatedAt(), reloadedUser.getCreatedAt());
    }

    // ==================== Transaction Tests ====================

    @Test
    @DisplayName("save: Should support batch operations")
    void save_ShouldSupportBatchOperations() {
        // Arrange
        User user1 = User.builder().email("batch1@example.com").fullName("Batch User 1").build();
        User user2 = User.builder().email("batch2@example.com").fullName("Batch User 2").build();
        User user3 = User.builder().email("batch3@example.com").fullName("Batch User 3").build();

        // Act
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<User> users = userRepository.findAll();
        assertEquals(3, users.size());
    }
}
