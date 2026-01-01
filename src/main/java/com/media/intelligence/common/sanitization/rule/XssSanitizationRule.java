package com.media.intelligence.common.sanitization.rule;

import com.media.intelligence.common.sanitization.core.SanitizationRule;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Removes XSS threats by stripping HTML/JavaScript and dangerous content.
 * <p>
 * This rule safely removes:
 * - Script tags and their content
 * - Style tags and their content
 * - Iframe, object, embed tags
 * - Event handlers (onclick, onerror, etc.)
 * - Javascript: protocol
 * - All other HTML tags (while preserving safe text content)
 * <p>
 * Non-HTML text (like plain email addresses) is preserved as-is.
 */
public class XssSanitizationRule extends SanitizationRule<String> {
    public XssSanitizationRule() {
        super("XSS_SANITIZATION");
    }

    @Override
    protected String sanitizeValue(String value) {
        // If the value doesn't contain HTML-like patterns, return as-is
        if (!containsHtmlPattern(value)) {
            return value;
        }

        // Use Jsoup.clean directly with a safelist that removes all tags
        // This preserves text content while removing all HTML tags and attributes
        Document.OutputSettings outputSettings = new Document.OutputSettings()
                .prettyPrint(false);

        // Safelist.none() removes all HTML tags but keeps text content
        // This includes dangerous tags like <script>, <iframe>, etc.
        return Jsoup.clean(value, "", Safelist.none(), outputSettings);
    }

    /**
     * Check if the value contains valid HTML tag patterns.
     * This helps avoid processing plain text through HTML parser.
     * <p>
     * Valid patterns include:
     * - Opening tags: &lt;tag&gt; or &lt;tag attr="value"&gt;
     * - Closing tags: &lt;/tag&gt;
     * - Self-closing tags: &lt;tag/&gt;
     */
    private boolean containsHtmlPattern(String value) {
        // Check for valid HTML tag patterns using regex
        // Pattern matches: <word> or <word attributes> or </word> or <word/>
        return value.matches(".*</?\\w+(?:\\s+[^>]*)?>.*");
    }
}
