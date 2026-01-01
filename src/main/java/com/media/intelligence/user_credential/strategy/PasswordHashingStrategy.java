package com.media.intelligence.user_credential.strategy;

/**
 * Strategy interface for password hashing algorithms.
 * Allows different hashing strategies (SHA-512, BCrypt, Argon2, etc.) to be used interchangeably.
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
}
