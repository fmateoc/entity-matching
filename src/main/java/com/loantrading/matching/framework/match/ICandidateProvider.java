package com.loantrading.matching.framework.match;

import com.loantrading.matching.framework.workflow.MatchingContext;
import java.util.List;

/**
 * An interface for components that provide a list of potential match candidates.
 *
 * Implementations of this interface are responsible for querying a "source of truth"
 * data store (e.g., a database, an API) to find a set of candidate records that
 * could potentially match the source entity. The goal is to perform an initial,
 * efficient filtering to narrow down the search space for the more intensive
 * matching strategies.
 *
 * @param <T> The type of the candidate objects to be provided.
 * @param <U> The type of the source object or query used to find candidates.
 */
public interface ICandidateProvider<T, U> {

    /**
     * Finds and returns a list of candidate records based on a source object or query.
     *
     * @param source The source object or query criteria to use for finding candidates.
     * @param context The current matching context, which may contain additional information
     *                to aid in the search (e.g., database connections, configuration).
     * @return A list of candidate objects.
     */
    List<T> findCandidates(U source, MatchingContext context);
}