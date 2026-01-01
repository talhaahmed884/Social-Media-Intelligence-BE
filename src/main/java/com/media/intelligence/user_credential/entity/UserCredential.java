package com.media.intelligence.user_credential.entity;

import com.media.intelligence.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Credential entity for storing password hashes.
 * Separated from User entity to follow Single Responsibility Principle.
 * <p>
 * Stores only the password hash - no account management fields.
 * <p>
 * Bidirectional @OneToOne relationship with User using shared primary key (@MapsId):
 * - UserCredential shares the same ID as User (enforces 1:1 relationship at DB level)
 * - User has cascade operations to UserCredential
 * - This approach prevents "transient instance" errors and ensures referential integrity
 */
@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredential {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(nullable = false)
    private String passwordHash;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Convenience getter for userId to maintain backward compatibility
    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }
}
