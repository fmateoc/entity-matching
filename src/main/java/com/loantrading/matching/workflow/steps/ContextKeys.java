package com.loantrading.matching.workflow.steps;

/**
 * Defines constant keys for storing and retrieving data from the {@link com.loantrading.matching.framework.workflow.MatchingContext}.
 * This avoids the use of "magic strings" and provides a centralized definition of all context variables.
 */
public final class ContextKeys {

    private ContextKeys() {
        // Prevent instantiation
    }

    // Input Data
    public static final String ADF_CONTENT = "adfContent";
    public static final String ADF_FILENAME = "adfFilename";
    public static final String TAX_FORM_CONTENT = "taxFormContent";
    public static final String TAX_FORM_FILENAME = "taxFormFilename";

    // Processing State
    public static final String PROCESSING_RESULT = "processingResult";
    public static final String START_TIME_MS = "startTimeMs";

    // Extracted Data
    public static final String EXTRACTED_ADF_DATA = "extractedAdfData";
    public static final String EXTRACTED_TAX_FORM_DATA = "extractedTaxFormData";
    public static final String MASTER_ENTITY = "masterEntity";

    // Detected Information
    public static final String ENTITY_TYPE = "entityType";

    // Matching Results
    public static final String MATCH_RESULTS = "matchResults";
    public static final String BEST_MATCH = "bestMatch";

    // Final Decision
    public static final String DECISION = "decision";
}