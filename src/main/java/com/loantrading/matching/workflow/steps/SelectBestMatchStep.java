package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.entity.MatchResult;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;

import java.util.List;
import java.util.Optional;

/**
 * A workflow step that selects the best match from a list of candidates.
 * It simply takes the top-scoring result and places it into the context.
 */
public class SelectBestMatchStep implements IWorkflowStep {

    @Override
    public void execute(MatchingContext context) {
        // We get the result as an Object and cast it to avoid generic type erasure issues.
        Optional<Object> matchesRaw = context.get(ContextKeys.MATCH_RESULTS, Object.class);

        if (matchesRaw.isEmpty()) {
            return; // No matches found
        }

        @SuppressWarnings("unchecked")
        List<MatchResult> matches = (List<MatchResult>) matchesRaw.get();

        if (matches.isEmpty()) {
            return; // No matches found
        }

        // The list is already sorted by the strategy, so the best match is the first one.
        MatchResult bestMatch = matches.get(0);
        context.put(ContextKeys.BEST_MATCH, bestMatch);
    }
}