package com.media.intelligence.user_credential.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Credential entity for storing password hashes.
 * Separated from User entity to follow Single Responsibility Principle.
 * <p>
 * Stores only the password hash - no account management fields.
 */
@Entity
@Table(name = "user_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private String passwordHash;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
