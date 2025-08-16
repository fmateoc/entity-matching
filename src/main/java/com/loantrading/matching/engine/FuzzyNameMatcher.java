package com.loantrading.matching.engine;

import com.loantrading.matching.entity.*;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Performs fuzzy name matching between extracted and LoanIQ entities
 */
public class FuzzyNameMatcher {
    private static final Logger logger = LoggerFactory.getLogger(FuzzyNameMatcher.class);
    
    private static final double LEGAL_NAME_THRESHOLD = 0.85;
    private static final double FUND_MANAGER_THRESHOLD = 0.70;
    
    private final JaroWinklerDistance jaroWinkler;
    private final LevenshteinDistance levenshtein;
    private final NameNormalizer normalizer;
    
    public FuzzyNameMatcher() {
        this.jaroWinkler = new JaroWinklerDistance();
        this.levenshtein = new LevenshteinDistance();
        this.normalizer = new NameNormalizer();
    }
    
    /**
     * Match extracted entity against a LoanIQ candidate using fuzzy name matching
     */
    public MatchResult match(ExtractedEntity extracted, LoanIQEntity candidate) {
        MatchResult result = new MatchResult();
        result.setMatchedEntity(candidate);
        
        double legalNameScore = 0;
        double fundManagerScore = 0;
        
        // Match legal entity name (strict threshold)
        if (extracted.getLegalName() != null && candidate.getFullName() != null) {
            legalNameScore = matchLegalName(extracted, candidate, result);
        }
        
        // Match fund manager (lenient threshold)
        if (extracted.getFundManager() != null && candidate.getUltimateParent() != null) {
            fundManagerScore = matchFundManager(extracted, candidate, result);
            result.setCompositeMatch(true);
        } else if (extracted.getFundManager() == null && candidate.getUltimateParent() == null) {
            // Both are standalone entities
            fundManagerScore = 1.0;
            result.setCompositeMatch(false);
        } else {
            // One has fund manager, other doesn't - penalty
            fundManagerScore = 0.3;
            result.addDiscrepancy(new Discrepancy(
                "ENTITY_TYPE_MISMATCH",
                DiscrepancySeverity.MEDIUM,
                "Entity type mismatch (managed vs standalone)"
            ));
        }
        
        // Calculate composite score
        double compositeScore = calculateCompositeScore(
            legalNameScore, fundManagerScore, result.isCompositeMatch()
        );
        
        result.setScore(compositeScore * 100);
        result.addScoreComponent("legal_name_fuzzy", legalNameScore * 70);
        result.addScoreComponent("fund_manager_fuzzy", fundManagerScore * 30);
        
        logger.debug("Fuzzy match result: {} (legal: {}, fm: {}, composite: {})",
            candidate.getFullName(), legalNameScore, fundManagerScore, compositeScore);
        
        return result;
    }
    
    private double matchLegalName(ExtractedEntity extracted, LoanIQEntity candidate,
                                 MatchResult result) {
        String normalizedExtracted = normalizer.normalize(extracted.getLegalName());
        String normalizedCandidate = normalizer.normalize(candidate.getFullName());
        
        // Check for DBA matching first
        double dbaScore = matchDBA(extracted, candidate);
        if (dbaScore > 0.85) {
            result.addEvidence("DBA match detected");
            return dbaScore;
        }
        
        // Use multiple similarity metrics
        double jwScore = jaroWinkler.apply(normalizedExtracted, normalizedCandidate);
        
        // Boost if normalized forms are exact match
        if (normalizedExtracted.equals(normalizedCandidate)) {
            result.addEvidence("Legal name exact match after normalization");
            return 1.0;
        }
        
        // Check for subset matching (one name contains the other)
        if (normalizedExtracted.contains(normalizedCandidate) ||
            normalizedCandidate.contains(normalizedExtracted)) {
            result.addEvidence("Legal name subset match");
            return Math.max(jwScore, 0.85);
        }
        
        // Check for word reordering
        if (areWordsReordered(normalizedExtracted, normalizedCandidate)) {
            result.addEvidence("Legal name match with word reordering");
            return Math.max(jwScore, 0.80);
        }
        
        if (jwScore > LEGAL_NAME_THRESHOLD) {
            result.addEvidence(String.format("Legal name fuzzy match (%.2f)", jwScore));
        } else if (jwScore > 0.7) {
            result.addEvidence(String.format("Legal name partial match (%.2f)", jwScore));
        }
        
        return jwScore;
    }
    
    private double matchFundManager(ExtractedEntity extracted, LoanIQEntity candidate,
                                   MatchResult result) {
        String normalizedExtractedFM = normalizer.normalizeFundManager(extracted.getFundManager());
        String normalizedCandidateFM = normalizer.normalizeFundManager(candidate.getUltimateParent());
        
        double fmScore = jaroWinkler.apply(normalizedExtractedFM, normalizedCandidateFM);
        
        // Check for common abbreviations
        if (areCommonAbbreviations(normalizedExtractedFM, normalizedCandidateFM)) {
            fmScore = Math.max(fmScore, 0.9);
            result.addEvidence("Fund manager abbreviation match");
        }
        
        // Check for subset matching
        if (normalizedExtractedFM.contains(normalizedCandidateFM) ||
            normalizedCandidateFM.contains(normalizedExtractedFM)) {
            fmScore = Math.max(fmScore, 0.85);
            result.addEvidence("Fund manager subset match");
        }
        
        if (fmScore > FUND_MANAGER_THRESHOLD) {
            result.addEvidence(String.format("Fund manager fuzzy match (%.2f)", fmScore));
        }
        
        return fmScore;
    }
    
    private double matchDBA(ExtractedEntity extracted, LoanIQEntity candidate) {
        // Parse DBA from LoanIQ format "Legal Name DBA Trade Name"
        String candidateName = candidate.getFullName();
        String[] dbaParts = candidateName.split("\\s+(?:DBA|d/b/a)\\s+", 2);
        
        if (dbaParts.length == 2) {
            String loanIQLegal = normalizer.normalize(dbaParts[0]);
            String loanIQDBA = normalizer.normalize(dbaParts[1]);
            
            if (extracted.getDba() != null) {
                // Check if extracted DBA matches LoanIQ DBA
                double dbaMatch = jaroWinkler.apply(
                    normalizer.normalize(extracted.getDba()),
                    loanIQDBA
                );
                
                if (dbaMatch > 0.85) {
                    return 0.95;
                }
            }
            
            // Check if extracted legal name matches either part
            if (extracted.getLegalName() != null) {
                String extractedNorm = normalizer.normalize(extracted.getLegalName());
                double legalMatch = jaroWinkler.apply(extractedNorm, loanIQLegal);
                double dbaMatch = jaroWinkler.apply(extractedNorm, loanIQDBA);
                return Math.max(legalMatch, dbaMatch);
            }
        }
        
        return 0;
    }
    
    private double calculateCompositeScore(double legalNameScore, double fundManagerScore,
                                          boolean isComposite) {
        if (isComposite) {
            // For managed funds: both components must match well
            if (legalNameScore < 0.7 || fundManagerScore < 0.6) {
                // Poor match on either component
                return Math.min(legalNameScore, fundManagerScore) * 0.5;
            } else {
                // Good match on both - weighted average
                return (legalNameScore * 0.7) + (fundManagerScore * 0.3);
            }
        } else {
            // For standalone entities: only legal name matters
            return legalNameScore;
        }
    }
    
    private boolean areWordsReordered(String name1, String name2) {
        String[] words1 = name1.split("\\s+");
        String[] words2 = name2.split("\\s+");
        
        if (words1.length != words2.length) {
            return false;
        }
        
        Arrays.sort(words1);
        Arrays.sort(words2);
        
        return Arrays.equals(words1, words2);
    }
    
    private boolean areCommonAbbreviations(String name1, String name2) {
        // Check if one is abbreviation of the other
        String[] words1 = name1.split("\\s+");
        String[] words2 = name2.split("\\s+");
        
        // Check acronym
        if (words1.length == 1 && words2.length > 1) {
            String acronym = Arrays.stream(words2)
                .map(w -> w.length() > 0 ? String.valueOf(w.charAt(0)) : "")
                .collect(Collectors.joining());
            if (words1[0].equalsIgnoreCase(acronym)) {
                return true;
            }
        }
        
        // Reverse check
        if (words2.length == 1 && words1.length > 1) {
            String acronym = Arrays.stream(words1)
                .map(w -> w.length() > 0 ? String.valueOf(w.charAt(0)) : "")
                .collect(Collectors.joining());
            if (words2[0].equalsIgnoreCase(acronym)) {
                return true;
            }
        }
        
        return false;
    }
}