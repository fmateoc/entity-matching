package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.entity.ProcessingResult;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;

/**
 * Workflow step to finalize the processing. It calculates the total
 * processing time and sets a final decision if one hasn't been determined yet.
 */
public class FinalizeProcessingStep implements IWorkflowStep {

    @Override
    public void execute(MatchingContext context) {
        ProcessingResult result = context.getRequired(ContextKeys.PROCESSING_RESULT, ProcessingResult.class);
        long startTime = context.getRequired(ContextKeys.START_TIME_MS, Long.class);

        // If no decision has been made by this point, it means no matches were found.
        if (result.getDecision() == null) {
            // Check if there was an error during pre-validation
            if (!"ERROR".equals(result.getDecision())) {
                result.setDecision("NO_MATCH");
                result.addAuditEntry("No matches found - new entity");
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;
        result.setProcessingTimeMs(processingTime);
        result.addAuditEntry(String.format("Processing completed in %d ms", processingTime));
        result.addMetadata("processing_time_ms", processingTime);
    }
}