package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.entity.ProcessingResult;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;

/**
 * Workflow step to initialize the processing environment.
 * It creates the ProcessingResult object and records the start time.
 */
public class InitializeProcessingStep implements IWorkflowStep {

    @Override
    public void execute(MatchingContext context) {
        ProcessingResult result = new ProcessingResult();
        context.put(ContextKeys.PROCESSING_RESULT, result);
        context.put(ContextKeys.START_TIME_MS, System.currentTimeMillis());
    }
}