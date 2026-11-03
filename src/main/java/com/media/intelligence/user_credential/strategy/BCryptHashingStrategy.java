package com.media.intelligence.user_credential.strategy;

import com.media.intelligence.user_credential.exception.UserCredentialErrorCode;
import com.media.intelligence.user_credential.exception.UserCredentialException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * BCrypt password hashing strategy (salted, adaptive cost) and the application's only
 * {@link PasswordHashingStrategy} bean. Hashes look like {@code $2a$12$...}.
 * <p>
 * The cost comes from {@code user-credential.bcrypt-strength} (default 12). Raising it upgrades existing
 * hashes on their next successful verification (see {@link #needsRehash}).
 * <p>
 * Stored values that are not BCrypt hashes are rejected with INVALID_PASSWORD_HASH.
 * <p>
 * BCrypt only uses the first 72 bytes of a password; longer passwords are rejected when hashing
 * (validators cap passwords at 72 characters, this guards multi-byte input).
 */
@Component
public class BCryptHashingStrategy implements PasswordHashingStrategy {
    static final int MAX_PASSWORD_BYTES = 72;

    private final BCryptPasswordEncoder encoder;

    /**
     * @param strength log2 cost factor (4-31); 12 is a reasonable production default
     */
    public BCryptHashingStrategy(@Value("${user-credential.bcrypt-strength:12}") int strength) {
        this.encoder = new BCryptPasswordEncoder(strength);
    }

    /**
     * @return true if the value is a BCrypt hash
     */
    public static boolean isBCryptHash(String storedHash) {
        return storedHash != null && storedHash.matches("^\\$2[aby]?\\$\\d{2}\\$.{53}$");
    }

    @Override
    public String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_REQUIRED);
        }
        if (tooLong(password)) {
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_TOO_LONG);
        }
        return encoder.encode(password);
    }

    @Override
    public boolean verify(String password, String storedHash) {
        if (password == null || password.isEmpty()) {
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_REQUIRED);
        }
        if (storedHash == null || storedHash.isEmpty()) {
            throw new UserCredentialException(UserCredentialErrorCode.PASSWORD_HASH_EMPTY);
        }
        if (!isBCryptHash(storedHash)) {
            throw new UserCredentialException(UserCredentialErrorCode.INVALID_PASSWORD_HASH);
        }
        // A password that could never have been hashed cannot match
        return !tooLong(password) && encoder.matches(password, storedHash);
    }

    /**
     * True if the stored hash used a lower cost factor than currently configured.
     */
    @Override
    public boolean needsRehash(String storedHash) {
        return isBCryptHash(storedHash) && encoder.upgradeEncoding(storedHash);
    }

    private static boolean tooLong(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }
}
