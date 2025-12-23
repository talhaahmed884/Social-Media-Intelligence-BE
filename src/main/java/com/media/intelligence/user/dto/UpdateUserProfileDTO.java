package com.media.intelligence.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for updating user profile.
 * Contains fields that can be updated by the user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserProfileDTO {
    private String email;
    private String fullName;
}
