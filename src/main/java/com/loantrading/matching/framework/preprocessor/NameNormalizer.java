package com.loantrading.matching.framework.preprocessor;

import com.ibm.icu.text.Transliterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A normalizer for entity names that cleans, standardizes, and simplifies them
 * for more effective matching. This class implements the INormalizer interface
 * and provides advanced name normalization capabilities.
 */
public class NameNormalizer implements INormalizer<String> {

    private static final Map<String, String> ABBREVIATIONS = Map.of(
            "intl", "international",
            "natl", "national",
            "svcs", "services",
            "fin", "financial",
            "grp", "group",
            "tech", "technology"
    );

    private static final Pattern DBA_PATTERN = Pattern.compile("\\s+(d/b/a|dba)\\s+", Pattern.CASE_INSENSITIVE);
    private static final String PUNCTUATION_REGEX = "[^\\p{L}\\p{N}\\s-']"; // Preserving apostrophe
    private static final String CORPORATE_FORMS_REGEX = "\\b(inc|llc|co|plc|ltd|corp|limited|corporation|incorporated|industries|holdings)\\b[.,]*";
    private static final String STOP_WORDS_REGEX = "\\b(with)\\b";
    private static final Transliterator UNACCENT_TRANSLITERATOR = Transliterator.getInstance("Any-Latin; NFD; [:M:] Remove");

    /**
     * Inner class to hold the components of a name with a DBA clause.
     */
    public static class DBAComponents {
        public final String legalName;
        public final String tradeName;

        public DBAComponents(String legalName, String tradeName) {
            this.legalName = legalName;
            this.tradeName = tradeName;
        }

        public boolean hasDBA() {
            return tradeName != null;
        }
    }

    /**
     * Inner class to hold the core name and the corporate form.
     */
    public static class NameComponents {
        public final String coreName;
        public final String corporateForm;

        public NameComponents(String coreName, String corporateForm) {
            this.coreName = coreName;
            this.corporateForm = corporateForm;
        }
    }

    @Override
    public String normalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Step 1: Unicode and smart punctuation normalization
        String normalized = unaccent(input);
        normalized = normalizeSmartPunctuation(normalized);

        // Step 2: Convert to lowercase
        normalized = normalized.toLowerCase();

        // Step 3: Remove corporate forms
        normalized = normalized.replaceAll(CORPORATE_FORMS_REGEX, "");

        // Step 4: Remove punctuation (except hyphens)
        normalized = normalized.replaceAll(PUNCTUATION_REGEX, "");

        // Step 5: Expand abbreviations
        for (Map.Entry<String, String> entry : ABBREVIATIONS.entrySet()) {
            normalized = normalized.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }

        // Step 6: Remove stop words
        normalized = normalized.replaceAll(STOP_WORDS_REGEX, "");

        // Step 7: Normalize whitespace
        return normalized.trim().replaceAll("\\s+", " ");
    }

    /**
     * Extracts the legal name and "Doing Business As" (DBA) name from a string.
     * @param input The full name string.
     * @return A DBAComponents object containing the separated names.
     */
    public DBAComponents extractDBA(String input) {
        if (input == null) {
            return new DBAComponents(null, null);
        }
        Matcher matcher = DBA_PATTERN.matcher(input);
        if (matcher.find()) {
            String legalName = input.substring(0, matcher.start()).trim();
            String tradeName = input.substring(matcher.end()).trim();
            return new DBAComponents(legalName, tradeName);
        } else {
            return new DBAComponents(input, null);
        }
    }

    /**
     * Extracts the core name and corporate form from a string.
     * @param input The full name string.
     * @return A NameComponents object containing the separated parts.
     */
    public NameComponents extractNameComponents(String input) {
        if (input == null || input.isBlank()) {
            return new NameComponents("", null);
        }
        String lowercasedInput = input.toLowerCase();
        Pattern pattern = Pattern.compile(CORPORATE_FORMS_REGEX);
        Matcher matcher = pattern.matcher(lowercasedInput);

        if (matcher.find()) {
            String corporateForm = matcher.group(1);
            String coreName = input.replaceAll("(?i)" + CORPORATE_FORMS_REGEX, "").trim();
            return new NameComponents(coreName, corporateForm);
        } else {
            return new NameComponents(input, null);
        }
    }

    private String unaccent(String text) {
        return UNACCENT_TRANSLITERATOR.transliterate(text);
    }

    private String normalizeSmartPunctuation(String text) {
        return text
                .replace('’', '\'')
                .replace('‘', '\'')
                .replace('”', '"')
                .replace('“', '"')
                .replace('—', '-') // em dash
                .replace('–', '-'); // en dash
    }
}