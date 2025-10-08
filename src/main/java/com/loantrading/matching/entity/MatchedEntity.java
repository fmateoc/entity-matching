package com.loantrading.matching.entity;

/**
 * Represents an entity from the LoanIQ database that has been identified as a potential match.
 * This class extends LoanIQEntity, inheriting its fields. It serves as a distinct type
 * for use in the matching framework to clarify that this entity is a candidate in a matching process.
 */
public class MatchedEntity extends LoanIQEntity {
    // This class can be extended with additional fields specific to the matching process if needed,
    // such as interim scores or match-specific metadata. For now, it serves as a type marker.
}