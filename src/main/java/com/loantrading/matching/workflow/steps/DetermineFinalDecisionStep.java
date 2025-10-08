package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.entity.MatchResult;
import com.loantrading.matching.entity.ProcessingResult;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;

import java.util.Optional;

/**
 * A workflow step that makes the final decision based on the best match's score and discrepancies.
 */
public class DetermineFinalDecisionStep implements IWorkflowStep {

    @Override
    public void execute(MatchingContext context) {
        ProcessingResult result = context.getRequired(ContextKeys.PROCESSING_RESULT, ProcessingResult.class);
        Optional<MatchResult> bestMatchOpt = context.get(ContextKeys.BEST_MATCH, MatchResult.class);

        if (bestMatchOpt.isEmpty()) {
            // If there's no best match, the FinalizeProcessingStep will set the decision to NO_MATCH.
            return;
        }

        MatchResult bestMatch = bestMatchOpt.get();
        result.setSelectedMatch(bestMatch); // Also set it on the final result object.

        String decision = determineDecision(bestMatch);
        result.setDecision(decision);

        // Log the final decision details
        result.addAuditEntry(String.format(
                "Best match selected: %s (ID: %d, Score: %.2f, Confidence: %s, Decision: %s)",
                bestMatch.getMatchedEntity().getFullName(),
                bestMatch.getMatchedEntity().getEntityId(),
                bestMatch.getScore(),
                bestMatch.getConfidence(),
                decision
        ));
    }

    private String determineDecision(MatchResult match) {
        double score = match.getScore();

        if (score >= 85) {
            if (match.hasCriticalDiscrepancies()) {
                return "MANUAL_REVIEW"; // High score but with critical issue needs review
            }
            return "MATCH";
        } else if (score >= 70) {
            return "MANUAL_REVIEW";
        } else {
            if (score >= 50) {
                return "MANUAL_REVIEW";
            } else {
                return "NO_MATCH";
            }
        }
    }
}