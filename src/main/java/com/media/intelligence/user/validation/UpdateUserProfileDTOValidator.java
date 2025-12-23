package com.media.intelligence.user.validation;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.common.validation.core.Validator;
import com.media.intelligence.common.validation.rule.EmailFormatRule;
import com.media.intelligence.common.validation.rule.RequiredRule;
import com.media.intelligence.common.validation.rule.StringLengthRule;
import com.media.intelligence.user.dto.UpdateUserProfileDTO;
import org.springframework.stereotype.Component;

/**
 * Validator for UpdateUserProfileDTO that combines validation rules.
 * <p>
 * Validation rules:
 * - email: Required, valid email format, max 255 characters
 * - fullName: Required, 2-255 characters
 */
@Component
public class UpdateUserProfileDTOValidator {
    private final Validator<UpdateUserProfileDTO> validator;

    public UpdateUserProfileDTOValidator() {
        this.validator = new Validator<UpdateUserProfileDTO>()
                .field("email",
                        new RequiredRule<>(),
                        new EmailFormatRule(),
                        new StringLengthRule(1, 255))
                .field("fullName",
                        new RequiredRule<>(),
                        new StringLengthRule(2, 255));
    }

    /**
     * Validate the UpdateUserProfileDTO.
     *
     * @param dto the DTO to validate
     * @return ValidationResult containing errors or success
     */
    public ValidationResult validate(UpdateUserProfileDTO dto) {
        return validator.validate(dto);
    }
}
