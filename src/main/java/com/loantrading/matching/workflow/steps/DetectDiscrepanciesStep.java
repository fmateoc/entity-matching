package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.discrepancy.DiscrepancyDetector;
import com.loantrading.matching.entity.Discrepancy;
import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.MatchedEntity;
import com.loantrading.matching.entity.MatchResult;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;

import java.util.List;
import java.util.Optional;

/**
 * A workflow step that detects discrepancies between the source data and the best matched entity.
 */
public class DetectDiscrepanciesStep implements IWorkflowStep {

    private final DiscrepancyDetector discrepancyDetector;

    public DetectDiscrepanciesStep(DiscrepancyDetector discrepancyDetector) {
        this.discrepancyDetector = discrepancyDetector;
    }

    @Override
    public void execute(MatchingContext context) {
        Optional<MatchResult> bestMatchOpt = context.get(ContextKeys.BEST_MATCH, MatchResult.class);
        Optional<ExtractedEntity> masterEntityOpt = context.get(ContextKeys.MASTER_ENTITY, ExtractedEntity.class);

        // If there's no best match or no master entity, there's nothing to compare.
        if (bestMatchOpt.isEmpty() || masterEntityOpt.isEmpty()) {
            return;
        }

        MatchResult bestMatch = bestMatchOpt.get();
        ExtractedEntity masterEntity = masterEntityOpt.get();

        // Cast the generic LoanIQEntity from the result to the specific MatchedEntity type.
        List<Discrepancy> discrepancies = discrepancyDetector.detect(masterEntity, (MatchedEntity) bestMatch.getMatchedEntity());

        if (!discrepancies.isEmpty()) {
            // Add the found discrepancies to the MatchResult object.
            discrepancies.forEach(bestMatch::addDiscrepancy);
        }
    }
}