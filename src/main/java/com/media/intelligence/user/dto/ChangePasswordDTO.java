package com.media.intelligence.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for changing user password.
 * Requires current password for verification and new password.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordDTO {
    private String currentPassword;
    private String newPassword;
}
