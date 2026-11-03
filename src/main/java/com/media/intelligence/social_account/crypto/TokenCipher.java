package com.media.intelligence.social_account.crypto;

import com.media.intelligence.social_account.config.SocialAccountProperties;
import com.media.intelligence.social_account.exception.SocialAccountErrorCode;
import com.media.intelligence.social_account.exception.SocialAccountException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts access tokens at rest with AES-GCM.
 * <p>
 * Output format: {@code base64(12-byte IV || ciphertext+tag)}. A fresh random IV is used per value.
 * <p>
 * The key comes from {@code social-account.token-encryption-key}. If it is not set the app still
 * starts, but linking or using social accounts fails with TOKEN_ENCRYPTION_NOT_CONFIGURED.
 * Changing the key makes existing tokens unreadable (accounts must be re-linked).
 */
@Slf4j
@Component
public class TokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenCipher(SocialAccountProperties properties) {
        this.key = parseKey(properties.tokenEncryptionKey());
        if (key == null) {
            log.warn("social-account.token-encryption-key is not set; social account linking is disabled");
        }
    }

    /**
     * @return true if an encryption key is configured
     */
    public boolean isConfigured() {
        return key != null;
    }

    /**
     * Encrypt a token.
     *
     * @param plaintext token
     * @return encoded ciphertext
     */
    public String encrypt(String plaintext) {
        requireKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array());
        } catch (GeneralSecurityException e) {
            throw new SocialAccountException(SocialAccountErrorCode.TOKEN_ENCRYPTION_FAILED, e);
        }
    }

    /**
     * Decrypt a value produced by {@link #encrypt(String)}.
     *
     * @param encoded encoded ciphertext
     * @return token
     */
    public String decrypt(String encoded) {
        requireKey();
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            if (combined.length <= IV_LENGTH) {
                throw new SocialAccountException(SocialAccountErrorCode.TOKEN_ENCRYPTION_FAILED);
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, combined, 0, IV_LENGTH));
            byte[] plaintext = cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new SocialAccountException(SocialAccountErrorCode.TOKEN_ENCRYPTION_FAILED, e);
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new SocialAccountException(SocialAccountErrorCode.TOKEN_ENCRYPTION_NOT_CONFIGURED);
        }
    }

    private static SecretKey parseKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            return null;
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("social-account.token-encryption-key must be base64", e);
        }

        if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32) {
            throw new IllegalStateException(
                    "social-account.token-encryption-key must decode to 16, 24 or 32 bytes (got " + bytes.length + ")");
        }
        return new SecretKeySpec(bytes, "AES");
    }
}
