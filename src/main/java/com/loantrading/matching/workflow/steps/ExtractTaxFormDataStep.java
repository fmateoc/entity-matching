package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.ProcessingResult;
import com.loantrading.matching.extraction.MultiFormatDocumentExtractor;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Workflow step to extract data from the Tax Form, if it exists.
 * This step encapsulates the parallel execution and awaiting of the result.
 */
public class ExtractTaxFormDataStep implements IWorkflowStep {

    private static final Logger logger = LoggerFactory.getLogger(ExtractTaxFormDataStep.class);
    private final MultiFormatDocumentExtractor extractor;
    private final ExecutorService executorService;

    public ExtractTaxFormDataStep(MultiFormatDocumentExtractor extractor, ExecutorService executorService) {
        this.extractor = extractor;
        this.executorService = executorService;
    }

    @Override
    public void execute(MatchingContext context) {
        Optional<byte[]> taxFormContentOpt = context.get(ContextKeys.TAX_FORM_CONTENT, byte[].class);

        if (taxFormContentOpt.isEmpty()) {
            return; // No tax form to process
        }

        ProcessingResult result = context.getRequired(ContextKeys.PROCESSING_RESULT, ProcessingResult.class);
        String taxFormFilename = context.getRequired(ContextKeys.TAX_FORM_FILENAME, String.class);
        byte[] taxFormContent = taxFormContentOpt.get();

        result.addAuditEntry("Starting tax form extraction from: " + taxFormFilename);

        Future<ExtractedEntity> taxFormFuture = executorService.submit(() -> {
            try {
                return extractor.extract(taxFormContent, taxFormFilename);
            } catch (Exception e) {
                logger.error("Tax form extraction failed", e);
                return null;
            }
        });

        try {
            ExtractedEntity taxFormData = taxFormFuture.get(30, TimeUnit.SECONDS);
            if (taxFormData != null) {
                result.setTaxFormData(taxFormData);
                result.addAuditEntry(String.format("Tax form extraction complete. Confidence: %.2f", taxFormData.getExtractionConfidence()));
                context.put(ContextKeys.EXTRACTED_TAX_FORM_DATA, taxFormData);
            }
        } catch (TimeoutException e) {
            result.addAuditEntry("Tax form extraction timed out");
        } catch (Exception e) {
            logger.error("Failed to get tax form extraction result", e);
            result.addAuditEntry("Error during tax form extraction: " + e.getMessage());
        }
    }
}