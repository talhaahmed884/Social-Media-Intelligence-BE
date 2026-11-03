package com.media.intelligence.social_account.crypto;

import com.media.intelligence.social_account.exception.SocialAccountErrorCode;
import com.media.intelligence.social_account.exception.SocialAccountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static com.media.intelligence.social_account.SocialAccountTestFixtures.TEST_ENCRYPTION_KEY;
import static com.media.intelligence.social_account.SocialAccountTestFixtures.properties;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenCipher Tests")
public class TokenCipherTest {

    private final TokenCipher cipher = new TokenCipher(properties());

    @Test
    @DisplayName("encrypt/decrypt: Should round-trip and never store plaintext")
    void encryptDecrypt_ShouldRoundTrip() {
        // Act
        String encrypted = cipher.encrypt("IGQVJ-secret-token");

        // Assert
        assertTrue(cipher.isConfigured());
        assertFalse(encrypted.contains("secret"));
        assertEquals("IGQVJ-secret-token", cipher.decrypt(encrypted));
    }

    @Test
    @DisplayName("encrypt: Should use a fresh IV so equal tokens encrypt differently")
    void encrypt_ShouldUseFreshIv() {
        assertNotEquals(cipher.encrypt("same"), cipher.encrypt("same"));
    }

    @Test
    @DisplayName("decrypt: Should reject tampered ciphertext")
    void decrypt_ShouldRejectTampered() {
        // Arrange
        byte[] bytes = Base64.getDecoder().decode(cipher.encrypt("token"));
        bytes[bytes.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        // Act & Assert
        assertErrorCode(SocialAccountErrorCode.TOKEN_ENCRYPTION_FAILED, () -> cipher.decrypt(tampered));
        assertErrorCode(SocialAccountErrorCode.TOKEN_ENCRYPTION_FAILED, () -> cipher.decrypt("not-base64!"));
    }

    @Test
    @DisplayName("decrypt: Should fail with a different key")
    void decrypt_ShouldFailWithDifferentKey() {
        // Arrange
        String otherKey = Base64.getEncoder().encodeToString("another-key-another-key-12345678".getBytes());
        TokenCipher other = new TokenCipher(properties(otherKey, null));
        String encrypted = cipher.encrypt("token");

        // Act & Assert
        assertErrorCode(SocialAccountErrorCode.TOKEN_ENCRYPTION_FAILED, () -> other.decrypt(encrypted));
    }

    @Test
    @DisplayName("TokenCipher: Should be disabled without a key and fail on use")
    void tokenCipher_ShouldBeDisabledWithoutKey() {
        // Arrange
        TokenCipher disabled = new TokenCipher(properties(" ", null));

        // Act & Assert
        assertFalse(disabled.isConfigured());
        assertErrorCode(SocialAccountErrorCode.TOKEN_ENCRYPTION_NOT_CONFIGURED, () -> disabled.encrypt("token"));
    }

    @Test
    @DisplayName("TokenCipher: Should reject keys of invalid length at startup")
    void tokenCipher_ShouldRejectInvalidKeyLength() {
        String shortKey = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThrows(IllegalStateException.class, () -> new TokenCipher(properties(shortKey, null)));
        assertThrows(IllegalStateException.class, () -> new TokenCipher(properties("%%%", null)));
        assertDoesNotThrow(() -> new TokenCipher(properties(TEST_ENCRYPTION_KEY, null)));
    }

    private static void assertErrorCode(SocialAccountErrorCode expected, org.junit.jupiter.api.function.Executable executable) {
        SocialAccountException ex = assertThrows(SocialAccountException.class, executable);
        assertEquals(expected, ex.getErrorCode());
    }
}
