package com.media.intelligence.user.validation;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.common.validation.core.Validator;
import com.media.intelligence.common.validation.rule.EmailFormatRule;
import com.media.intelligence.common.validation.rule.RequiredRule;
import com.media.intelligence.common.validation.rule.StringLengthRule;
import com.media.intelligence.user.dto.FindUserByEmailDTO;
import org.springframework.stereotype.Component;

/**
 * Validator for FindUserByEmailDTO that combines validation rules.
 * <p>
 * Validation rules:
 * - email: Required, valid email format, max 255 characters
 */
@Component
public class FindUserByEmailDTOValidator {

    private final Validator<FindUserByEmailDTO> validator;

    public FindUserByEmailDTOValidator() {
        this.validator = new Validator<FindUserByEmailDTO>()
                .field("email",
                        new RequiredRule<>(),
                        new EmailFormatRule(),
                        new StringLengthRule(1, 255));
    }

    /**
     * Validate the FindUserByEmailDTO.
     *
     * @param dto the DTO to validate
     * @return ValidationResult containing errors or success
     */
    public ValidationResult validate(FindUserByEmailDTO dto) {
        return validator.validate(dto);
    }
}
