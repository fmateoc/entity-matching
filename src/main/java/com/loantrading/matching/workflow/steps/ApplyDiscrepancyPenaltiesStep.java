package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.entity.Discrepancy;
import com.loantrading.matching.entity.MatchResult;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;

import java.util.Optional;

/**
 * A workflow step to apply score penalties based on the severity of found discrepancies.
 */
public class ApplyDiscrepancyPenaltiesStep implements IWorkflowStep {

    @Override
    public void execute(MatchingContext context) {
        Optional<MatchResult> bestMatchOpt = context.get(ContextKeys.BEST_MATCH, MatchResult.class);

        if (bestMatchOpt.isEmpty() || bestMatchOpt.get().getDiscrepancies().isEmpty()) {
            return; // No match or no discrepancies to penalize.
        }

        MatchResult bestMatch = bestMatchOpt.get();
        double currentScore = bestMatch.getScore();
        double penalty = 0;

        for (Discrepancy discrepancy : bestMatch.getDiscrepancies()) {
            switch (discrepancy.getSeverity()) {
                case CRITICAL:
                    penalty += 30;
                    break;
                case HIGH:
                    penalty += 15;
                    break;
                case MEDIUM:
                    penalty += 5;
                    break;
                case LOW:
                    penalty += 1;
                    break;
            }
        }

        double newScore = Math.max(0, currentScore - penalty);
        bestMatch.setScore(newScore);

        context.getRequired(ContextKeys.PROCESSING_RESULT, com.loantrading.matching.entity.ProcessingResult.class)
                .addAuditEntry(String.format("Applied discrepancy penalty of %.2f. New score: %.2f", penalty, newScore));
    }
}