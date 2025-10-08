package com.loantrading.matching.workflow.strategies;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.MatchResult;
import com.loantrading.matching.entity.MatchedEntity;
import com.loantrading.matching.framework.match.IMatchingStrategy;
import com.loantrading.matching.framework.workflow.MatchingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A matching strategy that looks for an exact match on a unique identifier.
 * It checks for matches on MEI, LEI, and EIN in that order of precedence.
 */
public class IdentifierMatchStrategy implements IMatchingStrategy<ExtractedEntity, MatchedEntity> {

    private static final double PERFECT_SCORE = 100.0;
    private static final String STRATEGY_NAME = "IDENTIFIER_MATCH";

    @Override
    public List<MatchResult> match(ExtractedEntity source, List<MatchedEntity> candidates, MatchingContext context) {
        List<MatchResult> results = new ArrayList<>();

        // Check for MEI match
        if (findMatch(source, candidates, MatchedEntity::getMei, source.getMei(), "MEI", results)) {
            return results; // Return immediately on the highest-confidence match
        }
        // Check for LEI match
        if (findMatch(source, candidates, MatchedEntity::getLei, source.getLei(), "LEI", results)) {
            return results;
        }
        // Check for EIN match
        if (findMatch(source, candidates, MatchedEntity::getEin, source.getEin(), "EIN", results)) {
            return results;
        }

        return results; // Return empty list if no identifier match found
    }

    private boolean findMatch(ExtractedEntity source, List<MatchedEntity> candidates,
                              Function<MatchedEntity, String> identifierExtractor,
                              String sourceIdentifier,
                              String identifierName,
                              List<MatchResult> results) {
        if (sourceIdentifier == null || sourceIdentifier.isBlank()) {
            return false;
        }

        for (MatchedEntity candidate : candidates) {
            String candidateIdentifier = identifierExtractor.apply(candidate);
            if (Objects.equals(sourceIdentifier, candidateIdentifier)) {
                // Found a perfect match
                MatchResult result = new MatchResult();
                result.setMatchedEntity(candidate);
                result.setScore(PERFECT_SCORE);
                result.setMatchStrategy(STRATEGY_NAME + "_" + identifierName);
                results.add(result);
                return true; // Found a match, stop searching
            }
        }
        return false;
    }
}