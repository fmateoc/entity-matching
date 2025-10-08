package com.loantrading.matching.framework.persistence;

import java.util.Optional;

/**
 * An interface for a repository that persists and retrieves confirmed matches.
 *
 * This allows the system to "remember" high-confidence matches, such as linking
 * a specific Fund Manager name to a canonical entity ID in the database. This
 * persisted cache avoids the need to re-run expensive matching logic for the
* same parent entity in future batches or sessions.
 */
public interface IMatchRepository {

    /**
     * Saves a confirmed match to the persistent store.
     *
     * @param match The match to save.
     */
    void saveMatch(PersistedMatch match);

    /**
     * Finds a previously saved match from the persistent store.
     *
     * @param sourceEntityType The type of the source entity (e.g., "FUND_MANAGER").
     * @param sourceEntityIdentifier The unique identifier for the source entity (e.g., the manager's name).
     * @return An Optional containing the matched target entity ID if found, otherwise an empty Optional.
     */
    Optional<Long> findMatch(String sourceEntityType, String sourceEntityIdentifier);
}