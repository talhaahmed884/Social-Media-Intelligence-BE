package com.media.intelligence.user_credential.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object for changing user password.
 * Requires current password for verification and new password.
 * <p>
 * Lives in the credential domain (validated by ChangePasswordDTOValidator, sanitized by
 * ChangePasswordDTOSanitizer); the endpoint is exposed by UserController.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordDTO {
    @ToString.Exclude
    private String currentPassword;

    @ToString.Exclude
    private String newPassword;
}
