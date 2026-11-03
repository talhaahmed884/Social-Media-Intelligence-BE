package com.media.intelligence.user_credential.strategy;

/**
 * Strategy interface for password hashing algorithms.
 * The current implementation is {@link BCryptHashingStrategy}; another algorithm (e.g. Argon2) can be
 * added behind this interface.
 */
public interface PasswordHashingStrategy {
    /**
     * Hash a plaintext password.
     *
     * @param password the plaintext password
     * @return the hashed password (includes salt)
     */
    String hash(String password);

    /**
     * Verify a plaintext password against a stored hash.
     *
     * @param password   the plaintext password
     * @param storedHash the stored hash (includes salt)
     * @return true if password matches, false otherwise
     */
    boolean verify(String password, String storedHash);

    /**
     * Whether a stored hash should be replaced with a fresh {@link #hash(String)} after a successful
     * verification (e.g. an outdated cost factor).
     *
     * @param storedHash the stored hash
     * @return true if the hash should be upgraded
     */
    default boolean needsRehash(String storedHash) {
        return false;
    }
}
