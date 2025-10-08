package com.loantrading.matching.framework.match;

import com.loantrading.matching.entity.MatchResult;
import com.loantrading.matching.framework.workflow.MatchingContext;

import java.util.List;

/**
 * Represents a specific, atomic strategy for matching a source object against a list of candidates.
 *
 * An implementation of this interface encapsulates a single matching algorithm or rule,
 * such as an exact identifier match, a fuzzy name match, or a composite match.
 * A complete matching process is typically composed of a chain of these strategies.
 *
 * @param <T> The type of the source object to match.
 * @param <U> The type of the candidate objects to be matched against.
 */
public interface IMatchingStrategy<T, U> {

    /**
     * Executes the matching strategy.
     *
     * @param source The source object that needs to be matched.
     * @param candidates A list of candidate objects to match against.
     * @param context The current matching context, which may contain additional information
     *                or be used to store intermediate results.
     * @return A list of {@link MatchResult} objects, typically scored and ranked.
     */
    List<MatchResult> match(T source, List<U> candidates, MatchingContext context);
}