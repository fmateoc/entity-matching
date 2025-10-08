package com.loantrading.matching.framework.persistence;

/**
 * A data class representing a high-confidence match to be persisted.
 */
public class PersistedMatch {

    private final String sourceEntityType;
    private final String sourceEntityIdentifier;
    private final long targetEntityId;
    private final double confidenceScore;
    private final String matchedBy;

    public PersistedMatch(String sourceEntityType, String sourceEntityIdentifier, long targetEntityId, double confidenceScore, String matchedBy) {
        this.sourceEntityType = sourceEntityType;
        this.sourceEntityIdentifier = sourceEntityIdentifier;
        this.targetEntityId = targetEntityId;
        this.confidenceScore = confidenceScore;
        this.matchedBy = matchedBy;
    }

    public String getSourceEntityType() {
        return sourceEntityType;
    }

    public String getSourceEntityIdentifier() {
        return sourceEntityIdentifier;
    }

    public long getTargetEntityId() {
        return targetEntityId;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public String getMatchedBy() {
        return matchedBy;
    }
}