package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.ProcessingResult;
import com.loantrading.matching.extraction.ExtractionException;
import com.loantrading.matching.extraction.MultiFormatDocumentExtractor;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow step to extract data from the Administrative Details Form (ADF).
 */
public class ExtractAdfDataStep implements IWorkflowStep {

    private static final Logger logger = LoggerFactory.getLogger(ExtractAdfDataStep.class);
    private final MultiFormatDocumentExtractor extractor;

    public ExtractAdfDataStep(MultiFormatDocumentExtractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public void execute(MatchingContext context) {
        ProcessingResult result = context.getRequired(ContextKeys.PROCESSING_RESULT, ProcessingResult.class);
        byte[] adfContent = context.getRequired(ContextKeys.ADF_CONTENT, byte[].class);
        String adfFilename = context.getRequired(ContextKeys.ADF_FILENAME, String.class);

        try {
            result.addAuditEntry("Starting ADF extraction from: " + adfFilename);
            ExtractedEntity adfData = extractor.extract(adfContent, adfFilename);

            result.setExtractedData(adfData);
            result.addAuditEntry(String.format("ADF extraction complete. Confidence: %.2f", adfData.getExtractionConfidence()));
            // In a real implementation, we would call a shared "logExtractedIdentifiers" method here.

            context.put(ContextKeys.EXTRACTED_ADF_DATA, adfData);
        } catch (ExtractionException e) {
            logger.error("ADF extraction failed for {}", adfFilename, e);
            result.addAuditEntry("Critical Error: ADF extraction failed: " + e.getMessage());
            result.setDecision("ERROR"); // Halt processing by setting an error state
        }
    }
}