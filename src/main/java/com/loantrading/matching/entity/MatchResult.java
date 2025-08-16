package com.loantrading.matching.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of matching an extracted entity against LoanIQ
 */
public class MatchResult {
    private LoanIQEntity matchedEntity;
    private double score;
    private ConfidenceLevel confidence;
    private List<String> evidence;
    private List<Discrepancy> discrepancies;
    private Map<String, Double> scoreBreakdown;
    private boolean isCompositeMatch;
    private String matchStrategy;
    private List<LoanIQEntity> potentialDuplicates;
    
    public MatchResult() {
        this.evidence = new ArrayList<>();
        this.discrepancies = new ArrayList<>();
        this.scoreBreakdown = new HashMap<>();
        this.potentialDuplicates = new ArrayList<>();
    }
    
    // Getters and setters
    public LoanIQEntity getMatchedEntity() {
        return matchedEntity;
    }
    
    public void setMatchedEntity(LoanIQEntity matchedEntity) {
        this.matchedEntity = matchedEntity;
    }
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
        this.confidence = ConfidenceLevel.fromScore(score);
    }
    
    public ConfidenceLevel getConfidence() {
        return confidence;
    }
    
    public List<String> getEvidence() {
        return evidence;
    }
    
    public void addEvidence(String evidence) {
        this.evidence.add(evidence);
    }
    
    public List<Discrepancy> getDiscrepancies() {
        return discrepancies;
    }
    
    public void addDiscrepancy(Discrepancy discrepancy) {
        this.discrepancies.add(discrepancy);
    }
    
    public Map<String, Double> getScoreBreakdown() {
        return scoreBreakdown;
    }
    
    public void addScoreComponent(String component, double score) {
        this.scoreBreakdown.put(component, score);
    }
    
    public boolean isCompositeMatch() {
        return isCompositeMatch;
    }
    
    public void setCompositeMatch(boolean compositeMatch) {
        isCompositeMatch = compositeMatch;
    }
    
    public String getMatchStrategy() {
        return matchStrategy;
    }
    
    public void setMatchStrategy(String matchStrategy) {
        this.matchStrategy = matchStrategy;
    }
    
    public List<LoanIQEntity> getPotentialDuplicates() {
        return potentialDuplicates;
    }
    
    public void addPotentialDuplicate(LoanIQEntity duplicate) {
        this.potentialDuplicates.add(duplicate);
    }
    
    public boolean hasDiscrepancies() {
        return !discrepancies.isEmpty();
    }
    
    public boolean hasCriticalDiscrepancies() {
        return discrepancies.stream()
            .anyMatch(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL);
    }
    
    @Override
    public String toString() {
        return String.format("MatchResult{entity=%s, score=%.2f, confidence=%s, strategy=%s, discrepancies=%d}",
            matchedEntity != null ? matchedEntity.getFullName() : "null",
            score,
            confidence,
            matchStrategy,
            discrepancies.size());
    }


/**
 * Enum representing confidence levels for matching results
 */
private static enum ConfidenceLevel {
    HIGH(95, 100, "High Confidence", "Multiple exact identifiers + cross-source validation"),
    MEDIUM_HIGH(85, 94, "Medium-High Confidence", "Single strong identifier + supporting evidence"),
    MEDIUM(70, 84, "Medium Confidence", "Strong fuzzy matches with consistency"),
    REVIEW(0, 69, "Review Required", "Uncertain match requiring human review");
    
    private final int minScore;
    private final int maxScore;
    private final String displayName;
    private final String description;
    
    ConfidenceLevel(int minScore, int maxScore, String displayName, String description) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.displayName = displayName;
        this.description = description;
    }
    
    public static ConfidenceLevel fromScore(double score) {
        if (score >= 95) return HIGH;
        if (score >= 85) return MEDIUM_HIGH;
        if (score >= 70) return MEDIUM;
        return REVIEW;
    }
    
    public int getMinScore() {
        return minScore;
    }
    
    public int getMaxScore() {
        return maxScore;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean requiresReview() {
        return this == REVIEW;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%d-%d%%)", displayName, minScore, maxScore);
    }
}
}