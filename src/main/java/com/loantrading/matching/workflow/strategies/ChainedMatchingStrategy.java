package com.loantrading.matching.workflow.strategies;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.MatchResult;
import com.loantrading.matching.entity.MatchedEntity;
import com.loantrading.matching.framework.match.ICandidateProvider;
import com.loantrading.matching.framework.match.IMatchingStrategy;
import com.loantrading.matching.framework.workflow.MatchingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A "meta" matching strategy that executes a chain of other strategies in a defined order.
 * It replaces the monolithic logic of the old MatchingEngine with a configurable pipeline.
 */
public class ChainedMatchingStrategy implements IMatchingStrategy<ExtractedEntity, MatchedEntity> {

    private static final Logger logger = LoggerFactory.getLogger(ChainedMatchingStrategy.class);
    private final ICandidateProvider<MatchedEntity, ExtractedEntity> candidateProvider;
    private final List<IMatchingStrategy<ExtractedEntity, MatchedEntity>> strategies;

    public ChainedMatchingStrategy(
            ICandidateProvider<MatchedEntity, ExtractedEntity> candidateProvider,
            List<IMatchingStrategy<ExtractedEntity, MatchedEntity>> strategies) {
        this.candidateProvider = candidateProvider;
        this.strategies = strategies;
    }

    @Override
    public List<MatchResult> match(ExtractedEntity source, List<MatchedEntity> ignoredCandidates, MatchingContext context) {
        // 1. Get all potential candidates.
        List<MatchedEntity> candidates = candidateProvider.findCandidates(source, context);
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<MatchResult> allResults = new ArrayList<>();

        // 2. Execute each strategy in the chain.
        for (IMatchingStrategy<ExtractedEntity, MatchedEntity> strategy : strategies) {
            logger.debug("Executing strategy: {}", strategy.getClass().getSimpleName());
            List<MatchResult> strategyResults = strategy.match(source, candidates, context);

            if (!strategyResults.isEmpty()) {
                // If we find a perfect score, we can stop early.
                if (strategyResults.get(0).getScore() >= 100.0) {
                    logger.info("Found perfect match with strategy {}. Short-circuiting.", strategy.getClass().getSimpleName());
                    return strategyResults;
                }
                allResults.addAll(strategyResults);
            }
        }

        if (allResults.isEmpty()) {
            return List.of();
        }

        // 3. If no perfect match was found, sort all collected results and return the best one.
        allResults.sort(Comparator.comparing(MatchResult::getScore).reversed());
        return allResults;
    }
}