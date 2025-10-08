package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.MatchResult;
import com.loantrading.matching.entity.MatchedEntity;
import com.loantrading.matching.framework.match.IMatchingStrategy;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;

import java.util.List;
import java.util.Optional;

/**
 * A workflow step for handling the matching of simple, non-hierarchical (standalone) entities.
 * It wraps a given matching strategy and applies it to the master entity.
 */
public class StandaloneMatchingStep implements IWorkflowStep {

    private final IMatchingStrategy<ExtractedEntity, MatchedEntity> matchingStrategy;

    public StandaloneMatchingStep(IMatchingStrategy<ExtractedEntity, MatchedEntity> matchingStrategy) {
        this.matchingStrategy = matchingStrategy;
    }

    @Override
    public void execute(MatchingContext context) {
        Optional<ExtractedEntity> masterEntityOpt = context.get(ContextKeys.MASTER_ENTITY, ExtractedEntity.class);

        if (masterEntityOpt.isEmpty()) {
            return; // Pre-validation likely failed
        }

        List<MatchResult> matches = matchingStrategy.match(masterEntityOpt.get(), null, context);
        context.put(ContextKeys.MATCH_RESULTS, matches);
    }
}