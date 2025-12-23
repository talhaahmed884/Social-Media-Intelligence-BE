package com.media.intelligence.user.validation;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.common.validation.core.Validator;
import com.media.intelligence.common.validation.rule.PasswordStrengthRule;
import com.media.intelligence.common.validation.rule.RequiredRule;
import com.media.intelligence.user.dto.ChangePasswordDTO;
import org.springframework.stereotype.Component;

/**
 * Validator for ChangePasswordDTO that combines validation rules.
 * <p>
 * Validation rules:
 * - currentPassword: Required
 * - newPassword: Required, STRONG complexity (8+ chars, uppercase, lowercase, digit, special)
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
                        PasswordStrengthRule.strong());
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
