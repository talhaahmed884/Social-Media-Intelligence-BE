package com.media.intelligence.user.validation;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.common.validation.core.Validator;
import com.media.intelligence.common.validation.rule.EmailFormatRule;
import com.media.intelligence.common.validation.rule.PasswordStrengthRule;
import com.media.intelligence.common.validation.rule.RequiredRule;
import com.media.intelligence.common.validation.rule.StringLengthRule;
import com.media.intelligence.user.dto.RegisterUserDTO;
import org.springframework.stereotype.Component;

/**
 * Validator for RegisterUserDTO that combines validation rules.
 * <p>
 * Validation rules:
 * - email: Required, valid email format, max 255 characters
 * - password: Required, STRONG complexity (8+ chars, uppercase, lowercase, digit, special)
 * - fullName: Required, 2-255 characters
 */
@Component
public class RegisterUserDTOValidator {
    private final Validator<RegisterUserDTO> validator;

    public RegisterUserDTOValidator() {
        this.validator = new Validator<RegisterUserDTO>()
                .field("email",
                        new RequiredRule<>(),
                        new EmailFormatRule(),
                        new StringLengthRule(1, 255))
                .field("password",
                        new RequiredRule<>(),
                        PasswordStrengthRule.strong())
                .field("fullName",
                        new RequiredRule<>(),
                        new StringLengthRule(2, 255));
    }

    /**
     * Validate the RegisterUserDTO.
     *
     * @param dto the DTO to validate
     * @return ValidationResult containing errors or success
     */
    public ValidationResult validate(RegisterUserDTO dto) {
        return validator.validate(dto);
    }
}
