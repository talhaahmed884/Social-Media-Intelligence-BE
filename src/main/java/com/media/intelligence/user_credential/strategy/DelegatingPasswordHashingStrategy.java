package com.media.intelligence.user_credential.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The application's password hashing strategy (the only {@link PasswordHashingStrategy} bean).
 * <p>
 * - New hashes are BCrypt ({@code user-credential.bcrypt-strength}, default 12).
 * - Verification picks the algorithm from the stored hash's format, so legacy SHA-512 hashes keep working.
 * - {@link #needsRehash} is true for legacy hashes and for BCrypt hashes with a lower cost factor;
 * {@code UserCredentialService} re-hashes them after a successful verification.
 */
@Component
public class DelegatingPasswordHashingStrategy implements PasswordHashingStrategy {

    private final BCryptHashingStrategy bcrypt;
    private final SHA512HashingStrategy legacySha512;

    public DelegatingPasswordHashingStrategy(@Value("${user-credential.bcrypt-strength:12}") int bcryptStrength) {
        this.bcrypt = new BCryptHashingStrategy(bcryptStrength);
        this.legacySha512 = new SHA512HashingStrategy();
    }

    @Override
    public String hash(String password) {
        return bcrypt.hash(password);
    }

    @Override
    public boolean verify(String password, String storedHash) {
        return BCryptHashingStrategy.isBCryptHash(storedHash)
                ? bcrypt.verify(password, storedHash)
                : legacySha512.verify(password, storedHash);
    }

    @Override
    public boolean needsRehash(String storedHash) {
        return !BCryptHashingStrategy.isBCryptHash(storedHash) || bcrypt.needsRehash(storedHash);
    }
}
