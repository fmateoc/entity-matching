package com.loantrading.matching.workflow.strategies;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.MatchResult;
import com.loantrading.matching.entity.MatchedEntity;
import com.loantrading.matching.framework.match.IMatchingStrategy;
import com.loantrading.matching.framework.workflow.MatchingContext;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A matching strategy that uses fuzzy string matching on the entity name.
 * This is typically used when no exact identifier match can be found.
 */
public class FuzzyNameMatchStrategy implements IMatchingStrategy<ExtractedEntity, MatchedEntity> {

    private static final String STRATEGY_NAME = "FUZZY_NAME_MATCH";
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    @Override
    public List<MatchResult> match(ExtractedEntity source, List<MatchedEntity> candidates, MatchingContext context) {
        if (source.getLegalName() == null || source.getLegalName().isBlank()) {
            return List.of();
        }

        List<MatchResult> results = new ArrayList<>();
        String sourceName = normalize(source.getLegalName());

        for (MatchedEntity candidate : candidates) {
            if (candidate.getFullName() != null) {
                String candidateName = normalize(candidate.getFullName());
                double score = similarity.apply(sourceName, candidateName) * 100;

                // Only consider reasonably close matches
                if (score > 60) {
                    MatchResult result = new MatchResult();
                    result.setMatchedEntity(candidate);
                    result.setScore(score);
                    result.setMatchStrategy(STRATEGY_NAME);
                    results.add(result);
                }
            }
        }

        // Sort results by score in descending order
        results.sort(Comparator.comparing(MatchResult::getScore).reversed());

        return results;
    }

    private String normalize(String name) {
        // A simple normalizer. A real implementation would be more sophisticated.
        return name.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ").trim();
    }
}