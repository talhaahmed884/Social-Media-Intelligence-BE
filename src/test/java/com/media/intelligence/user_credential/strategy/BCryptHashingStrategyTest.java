package com.media.intelligence.user_credential.strategy;

import com.media.intelligence.user_credential.exception.UserCredentialErrorCode;
import com.media.intelligence.user_credential.exception.UserCredentialException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BCryptHashingStrategy Tests")
public class BCryptHashingStrategyTest {

    private static final int TEST_STRENGTH = 4;

    private final BCryptHashingStrategy bcrypt = new BCryptHashingStrategy(TEST_STRENGTH);

    @Test
    @DisplayName("hash/verify: Should hash with a random salt and verify")
    void hashVerify_ShouldHashAndVerify() {
        // Act
        String hash = bcrypt.hash("SecurePass123!");

        // Assert
        assertTrue(BCryptHashingStrategy.isBCryptHash(hash), hash);
        assertNotEquals(hash, bcrypt.hash("SecurePass123!"), "Each hash should use a new salt");
        assertTrue(bcrypt.verify("SecurePass123!", hash));
        assertFalse(bcrypt.verify("WrongPass123!", hash));
        assertFalse(bcrypt.needsRehash(hash), "A fresh hash should not need upgrading");
    }

    @Test
    @DisplayName("hash/verify: Should reject passwords over 72 bytes when hashing, and never match them")
    void hashVerify_ShouldRejectPasswordsOver72Bytes() {
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
    @DisplayName("needsRehash: Should be true only when the stored cost is lower than configured")
    void needsRehash_ShouldDetectOutdatedCost() {
        String lowCost = new BCryptHashingStrategy(4).hash("SecurePass123!");

        assertTrue(new BCryptHashingStrategy(5).needsRehash(lowCost));
        assertFalse(new BCryptHashingStrategy(4).needsRehash(lowCost));
        assertFalse(bcrypt.needsRehash("not-a-bcrypt-hash"), "Non-BCrypt values are rejected, not upgraded");
    }

    @Test
    @DisplayName("verify: Should reject non-BCrypt stored hashes and empty input")
    void verify_ShouldValidateInput() {
        assertEquals(UserCredentialErrorCode.INVALID_PASSWORD_HASH, assertThrows(UserCredentialException.class, () ->
                bcrypt.verify("SecurePass123!", "bm90LWEtYmNyeXB0LWhhc2g=")).getErrorCode());
        assertEquals(UserCredentialErrorCode.PASSWORD_HASH_EMPTY, assertThrows(UserCredentialException.class, () ->
                bcrypt.verify("SecurePass123!", "")).getErrorCode());
        assertEquals(UserCredentialErrorCode.PASSWORD_REQUIRED, assertThrows(UserCredentialException.class, () ->
                bcrypt.hash("")).getErrorCode());
    }
}
