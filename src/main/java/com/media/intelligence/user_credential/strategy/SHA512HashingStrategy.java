package com.media.intelligence.user_credential.strategy;

import com.media.intelligence.user_credential.exception.UserCredentialErrorCode;
import com.media.intelligence.user_credential.exception.UserCredentialException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SHA-512 password hashing strategy with salt.
 * <p>
 * Security Note: SHA-512 is fast, which makes it vulnerable to brute-force attacks.
 * For production, consider using BCrypt or Argon2 which are designed to be slow.
 */
@Slf4j
@Component
public class SHA512HashingStrategy implements PasswordHashingStrategy {

    private static final String ALGORITHM = "SHA-512";
    private static final int SALT_LENGTH = 16;

    @Override
    public String hash(String password) {
        try {
            if (password == null || password.isEmpty()) {
                throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_REQUIRED);
            }

            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // Hash password with salt
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // Combine salt and hash
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);

            String result = Base64.getEncoder().encodeToString(combined);
            log.debug("Password hashed successfully using {}", ALGORITHM);
            return result;
        } catch (NoSuchAlgorithmException e) {
            log.error("Hashing algorithm not found: {}", ALGORITHM, e);
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_HASH_FAILED, e);
        }
    }

    @Override
    public boolean verify(String password, String storedHash) {
        try {
            if (password == null || password.isEmpty()) {
                throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_REQUIRED);
            }

            if (storedHash == null || storedHash.isEmpty()) {
                throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_HASH_EMPTY);
            }

            byte[] combined = Base64.getDecoder().decode(storedHash);

            // Extract salt
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);

            // Extract stored hash
            byte[] storedHashBytes = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, SALT_LENGTH, storedHashBytes, 0, storedHashBytes.length);

            // Hash the provided password with the extracted salt
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // Compare using constant-time comparison
            boolean isValid = MessageDigest.isEqual(hashedPassword, storedHashBytes);
            log.debug("Password verification: {}", isValid ? "success" : "failed");
            return isValid;
        } catch (NoSuchAlgorithmException e) {
            log.error("Verification algorithm not found: {}", ALGORITHM, e);
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_VERIFICATION_FAILED, e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid hash format", e);
            throw new UserCredentialException(UserCredentialErrorCode.INVALID_PASSWORD_HASH, e);
        }
    }
}
