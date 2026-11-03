package com.media.intelligence.user_credential.strategy;

import com.media.intelligence.user_credential.exception.UserCredentialErrorCode;
import com.media.intelligence.user_credential.exception.UserCredentialException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Password Hashing Strategy Tests")
public class PasswordHashingStrategyTest {

    private static final int TEST_STRENGTH = 4;

    private final BCryptHashingStrategy bcrypt = new BCryptHashingStrategy(TEST_STRENGTH);
    private final SHA512HashingStrategy legacy = new SHA512HashingStrategy();
    private final DelegatingPasswordHashingStrategy delegating = new DelegatingPasswordHashingStrategy(TEST_STRENGTH);

    // ==================== BCryptHashingStrategy ====================

    @Test
    @DisplayName("BCrypt: Should hash with a random salt and verify")
    void bcrypt_ShouldHashAndVerify() {
        // Act
        String hash = bcrypt.hash("SecurePass123!");

        // Assert
        assertTrue(BCryptHashingStrategy.isBCryptHash(hash), hash);
        assertNotEquals(hash, bcrypt.hash("SecurePass123!"), "Each hash should use a new salt");
        assertTrue(bcrypt.verify("SecurePass123!", hash));
        assertFalse(bcrypt.verify("WrongPass123!", hash));
    }

    @Test
    @DisplayName("BCrypt: Should reject passwords over 72 bytes when hashing, and never match them")
    void bcrypt_ShouldRejectPasswordsOver72Bytes() {
        // Arrange: 36 two-byte characters = 72 bytes is the maximum
        String maxLength = "é".repeat(36);
        String tooLong = "é".repeat(37);
        String hash = bcrypt.hash(maxLength);

        // Act & Assert
        UserCredentialException ex = assertThrows(UserCredentialException.class, () -> bcrypt.hash(tooLong));
        assertEquals(UserCredentialErrorCode.PASSWORD_TOO_LONG, ex.getErrorCode());
        assertTrue(bcrypt.verify(maxLength, hash));
        assertFalse(bcrypt.verify(tooLong, hash));
    }

    @Test
    @DisplayName("BCrypt: Should report a rehash need when the stored cost is lower than configured")
    void bcrypt_ShouldDetectOutdatedCost() {
        String lowCost = new BCryptHashingStrategy(4).hash("SecurePass123!");

        assertTrue(new BCryptHashingStrategy(5).needsRehash(lowCost));
        assertFalse(new BCryptHashingStrategy(4).needsRehash(lowCost));
    }

    @Test
    @DisplayName("BCrypt: Should reject malformed hashes and empty input")
    void bcrypt_ShouldValidateInput() {
        assertEquals(UserCredentialErrorCode.INVALID_PASSWORD_HASH, assertThrows(UserCredentialException.class, () ->
                bcrypt.verify("SecurePass123!", "not-a-bcrypt-hash")).getErrorCode());
        assertEquals(UserCredentialErrorCode.PASSWORD_REQUIRED, assertThrows(UserCredentialException.class, () ->
                bcrypt.hash("")).getErrorCode());
    }

    // ==================== DelegatingPasswordHashingStrategy ====================

    @Test
    @DisplayName("Delegating: Should create BCrypt hashes that need no rehash")
    void delegating_ShouldHashWithBCrypt() {
        String hash = delegating.hash("SecurePass123!");

        assertTrue(BCryptHashingStrategy.isBCryptHash(hash));
        assertTrue(delegating.verify("SecurePass123!", hash));
        assertFalse(delegating.needsRehash(hash));
    }

    @Test
    @DisplayName("Delegating: Should verify legacy SHA-512 hashes and flag them for rehash")
    void delegating_ShouldVerifyLegacyHashes() {
        // Arrange: a hash created before the switch to BCrypt
        String legacyHash = legacy.hash("SecurePass123!");

        // Act & Assert
        assertFalse(BCryptHashingStrategy.isBCryptHash(legacyHash));
        assertTrue(delegating.verify("SecurePass123!", legacyHash));
        assertFalse(delegating.verify("WrongPass123!", legacyHash));
        assertTrue(delegating.needsRehash(legacyHash));
    }

    @Test
    @DisplayName("SHA-512: Should reject truncated hashes instead of crashing")
    void sha512_ShouldRejectTruncatedHash() {
        UserCredentialException ex = assertThrows(UserCredentialException.class, () ->
                legacy.verify("SecurePass123!", "c2hvcnQ="));

        assertEquals(UserCredentialErrorCode.INVALID_PASSWORD_HASH, ex.getErrorCode());
    }
}
