package com.media.intelligence.common.sanitization.rule;

import com.media.intelligence.common.sanitization.core.SanitizationRule;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * Removes XSS threats by stripping HTML/JavaScript.
 */
public class XssSanitizationRule extends SanitizationRule<String> {

    public XssSanitizationRule() {
        super("XSS_SANITIZATION");
    }

    @Override
    protected String sanitizeValue(String value) {
        // Remove all HTML tags and scripts
        return Jsoup.clean(value, Safelist.none());
    }
}
