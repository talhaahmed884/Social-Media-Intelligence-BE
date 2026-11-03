package com.media.intelligence.social_account.validation;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.common.validation.core.Validator;
import com.media.intelligence.common.validation.rule.RequiredRule;
import com.media.intelligence.common.validation.rule.StringLengthRule;
import com.media.intelligence.social_account.dto.LinkSocialAccountDTO;
import org.springframework.stereotype.Component;

/**
 * Validator for LinkSocialAccountDTO.
 * <p>
 * Validation rules:
 * - accessToken: Required, 1-1024 characters (fits the encrypted column)
 */
@Component
public class LinkSocialAccountDTOValidator {
    private final Validator<LinkSocialAccountDTO> validator;

    public LinkSocialAccountDTOValidator() {
        this.validator = new Validator<LinkSocialAccountDTO>()
                .field("accessToken",
                        new RequiredRule<>(),
                        new StringLengthRule(1, 1024));
    }

    public ValidationResult validate(LinkSocialAccountDTO dto) {
        return validator.validate(dto);
    }
}
