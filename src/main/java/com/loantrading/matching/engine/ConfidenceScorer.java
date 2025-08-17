package com.loantrading.matching.engine;

import com.loantrading.matching.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Calculates final confidence scores for matches
 */
public class ConfidenceScorer {
    private static final Logger logger = LoggerFactory.getLogger(ConfidenceScorer.class);
    
    /**
     * Calculate final confidence score for a match
     */
    public void calculateFinalScore(MatchResult match, ExtractedEntity extracted) {
        double score = 0;
        
        // Get base scores from components
        Map<String, Double> components = match.getScoreBreakdown();
        
        // Identifier-based scoring (40% weight)
        double identifierScore = calculateIdentifierScore(components);
        score += identifierScore;
        
        // Name matching scoring (30% weight)
        double nameScore = calculateNameScore(components, match.isCompositeMatch());
        score += nameScore;
        
        // Email domain scoring (20% weight)
        Double emailBoost = components.get("email_domain_boost");
        if (emailBoost != null) {
            score += emailBoost;
        }
        
        // Geographic consistency (10% weight)
        if (hasGeographicConsistency(extracted, match.getMatchedEntity())) {
            score += 10;
            match.addEvidence("Geographic data consistent");
        }
        
        // Apply penalties for discrepancies
        double penalty = calculateDiscrepancyPenalty(match.getDiscrepancies());
        score -= penalty;
        
        // Bonus for cross-source validation
        Double taxFormBonus = components.get("tax_form_validation");
        if (taxFormBonus != null) {
            score += taxFormBonus;
        }
        
        // Bonus for multiple identifier matches
        int identifierCount = countIdentifierMatches(components);
        if (identifierCount > 1) {
            score += (identifierCount - 1) * 5; // 5 points per additional identifier
            match.addEvidence(String.format("%d identifiers matched", identifierCount));
        }

        // Penalty for potential duplicates
        if (!match.getPotentialDuplicates().isEmpty()) {
            score -= 5; // Apply a small penalty
            match.addEvidence(String.format("Score penalized due to %d potential duplicates.",
                match.getPotentialDuplicates().size()));
        }
        
        // Ensure score is within bounds
        score = Math.max(0, Math.min(100, score));
        
        match.setScore(score);
        
        logger.debug("Final score for match: {} (identifier: {}, name: {}, penalty: {})",
            score, identifierScore, nameScore, penalty);
    }
    
    private double calculateIdentifierScore(Map<String, Double> components) {
        double score = 0;
        
        // MEI is most reliable
        if (components.containsKey("mei_match")) {
            score = 40;
        } else if (components.containsKey("lei_match")) {
            score = 35;
        } else if (components.containsKey("ein_match")) {
            score = 30;
        } else if (components.containsKey("debt_domain_id_match")) {
            score = 25;
        }
        
        // Add any identifier boosts
        score += components.getOrDefault("mei_boost", 0.0);
        score += components.getOrDefault("lei_boost", 0.0);
        score += components.getOrDefault("ein_boost", 0.0);
        score += components.getOrDefault("debt_domain_id_boost", 0.0);
        
        return score;
    }
    
    private double calculateNameScore(Map<String, Double> components, boolean isComposite) {
        double score = 0;
        
        Double legalNameScore = components.get("legal_name_fuzzy");
        Double fundManagerScore = components.get("fund_manager_fuzzy");
        
        if (isComposite) {
            // Both components must match well for composite entities
            if (legalNameScore != null && fundManagerScore != null) {
                if (legalNameScore > 60 && fundManagerScore > 20) {
                    // Good match on both
                    score = (legalNameScore * 0.7) + (fundManagerScore * 0.3);
                } else {
                    // Poor match on either component
                    score = Math.min(legalNameScore, fundManagerScore) * 0.5;
                }
            } else if (legalNameScore != null) {
                // Only legal name available
                score = legalNameScore * 0.5; // Reduced weight
            }
        } else {
            // Standalone entity - only legal name matters
            if (legalNameScore != null) {
                score = legalNameScore;
            }
        }
        
        return score * 0.3; // 30% weight
    }
    
    private double calculateDiscrepancyPenalty(java.util.List<Discrepancy> discrepancies) {
        double penalty = 0;
        
        for (Discrepancy disc : discrepancies) {
            penalty += disc.getSeverity().getScorePenalty();
        }
        
        // Cap penalty at 50 points
        return Math.min(penalty, 50);
    }
    
    private boolean hasGeographicConsistency(ExtractedEntity extracted, LoanIQEntity matched) {
        if (extracted.getCountryCode() == null || matched.getCountryCode() == null) {
            return true; // No data to contradict
        }
        
        // Check MEI country if available
        if (extracted.getMei() != null && matched.getMei() != null) {
            String extractedMeiCountry = extracted.getMei().substring(0, 2);
            String matchedMeiCountry = matched.getMei().substring(0, 2);
            return extractedMeiCountry.equals(matchedMeiCountry);
        }
        
        // Check address country codes
        return extracted.getCountryCode().equals(matched.getCountryCode());
    }
    
    private int countIdentifierMatches(Map<String, Double> components) {
        int count = 0;
        
        if (components.containsKey("mei_match") || components.containsKey("mei_boost")) {
            count++;
        }
        if (components.containsKey("lei_match") || components.containsKey("lei_boost")) {
            count++;
        }
        if (components.containsKey("ein_match") || components.containsKey("ein_boost")) {
            count++;
        }
        if (components.containsKey("debt_domain_id_match") || components.containsKey("debt_domain_id_boost")) {
            count++;
        }
        
        return count;
    }
}