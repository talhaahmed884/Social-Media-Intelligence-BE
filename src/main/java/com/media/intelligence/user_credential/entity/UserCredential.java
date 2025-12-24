package com.media.intelligence.user_credential.entity;

import com.media.intelligence.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Credential entity for storing password hashes.
 * Separated from User entity to follow Single Responsibility Principle.
 * <p>
 * Stores only the password hash - no account management fields.
 * <p>
 * Unidirectional @ManyToOne relationship to User:
 * - UserCredential knows about User (for FK constraint and cascade delete)
 * - User does NOT know about UserCredential (maintains loose coupling)
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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_user_credentials_user"))
    @OnDelete(action = OnDeleteAction.CASCADE)
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
