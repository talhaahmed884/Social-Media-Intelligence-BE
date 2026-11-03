package com.media.intelligence.user_credential.validation;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.common.validation.core.Validator;
import com.media.intelligence.common.validation.rule.PasswordStrengthRule;
import com.media.intelligence.common.validation.rule.RequiredRule;
import com.media.intelligence.common.validation.rule.StringLengthRule;
import com.media.intelligence.user_credential.dto.ChangePasswordDTO;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangePasswordDTO that combines validation rules.
 * <p>
 * Validation rules:
 * - currentPassword: Required only. It is checked against the stored hash, so it must not be held to
 * today's strength rules (a user with an older, weaker password must still be able to change it).
 * - newPassword: Required, STRONG complexity (8+ chars, uppercase, lowercase, digit, special), max 72 chars
 * (BCrypt limit)
 */
@Component
public class ChangePasswordDTOValidator {
    private final Validator<ChangePasswordDTO> validator;

    public ChangePasswordDTOValidator() {
        this.validator = new Validator<ChangePasswordDTO>()
                .field("currentPassword",
                        new RequiredRule<>())
                .field("newPassword",
                        new RequiredRule<>(),
                        PasswordStrengthRule.strong(),
                        new StringLengthRule(1, 72, "Password must be at most 72 characters"));
    }

    /**
     * Validate the ChangePasswordDTO.
     *
     * @param dto the DTO to validate
     * @return ValidationResult containing errors or success
     */
    public ValidationResult validate(ChangePasswordDTO dto) {
        return validator.validate(dto);
    }
}
