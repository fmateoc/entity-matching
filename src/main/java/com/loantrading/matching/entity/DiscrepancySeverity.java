package com.loantrading.matching.entity;

/**
 * Enum representing the severity of discrepancies found during matching
 */
public enum DiscrepancySeverity {
    CRITICAL("Critical", "Immediate review required", -25),
    HIGH("High", "Significant discrepancy", -15),
    MEDIUM("Medium", "Notable discrepancy", -10),
    LOW("Low", "Minor discrepancy", -5);
    
    private final String displayName;
    private final String description;
    private final int scorePenalty;
    
    DiscrepancySeverity(String displayName, String description, int scorePenalty) {
        this.displayName = displayName;
        this.description = description;
        this.scorePenalty = scorePenalty;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getScorePenalty() {
        return scorePenalty;
    }
    
    public boolean requiresImmediateAttention() {
        return this == CRITICAL || this == HIGH;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}