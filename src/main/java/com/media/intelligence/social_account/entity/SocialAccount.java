package com.media.intelligence.social_account.entity;

import com.media.intelligence.social_connector.provider.SocialPlatform;
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
 * A social platform account linked to an application user.
 * <p>
 * One user can link many accounts; the same platform account can be linked once per user
 * (re-linking updates the stored token). The access token is stored AES-GCM encrypted
 * (see {@code TokenCipher}); it is never exposed through the API.
 * <p>
 * Token timestamps ({@code tokenExpiresAt}, {@code tokenIssuedAt}) are UTC.
 */
@Entity
@Table(
        name = "social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_accounts_user_platform_account",
                        columnNames = {"user_id", "platform", "platform_account_id"}
                )
        },
        indexes = {
                @Index(name = "idx_social_accounts_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 32)
    private SocialPlatform platform;

    @Column(name = "platform_account_id", nullable = false, length = 255)
    private String platformAccountId;

    @Column(name = "username", length = 255)
    private String username;

    @ToString.Exclude
    @Column(name = "access_token_encrypted", nullable = false, length = 2048)
    private String accessTokenEncrypted;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "token_issued_at", nullable = false)
    private LocalDateTime tokenIssuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SocialAccountStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Owner's user ID without initializing the lazy user association.
     */
    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }
}
