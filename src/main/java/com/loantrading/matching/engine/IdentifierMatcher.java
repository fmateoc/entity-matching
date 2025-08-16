package com.loantrading.matching.engine;

import com.loantrading.matching.entity.*;
import com.loantrading.matching.repository.LoanIQRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches entities based on identifiers (MEI, LEI, EIN, Debt Domain ID)
 */
public class IdentifierMatcher {
    private static final Logger logger = LoggerFactory.getLogger(IdentifierMatcher.class);
    
    private final LoanIQRepository repository;
    
    public IdentifierMatcher(LoanIQRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Match extracted entity against LoanIQ using identifiers
     */
    public List<MatchResult> match(ExtractedEntity extracted) {
        List<MatchResult> matches = new ArrayList<>();
        
        // Priority 1: MEI matching (highest weight)
        if (extracted.getMei() != null) {
            logger.debug("Searching by MEI: {}", extracted.getMei());
            List<LoanIQEntity> meiMatches = repository.findByMEI(extracted.getMei());
            
            for (LoanIQEntity entity : meiMatches) {
                MatchResult result = createIdentifierMatch(entity, "MEI",
                    extracted.getMei(), 40.0);
                matches.add(result);
                logger.debug("Found MEI match: {} (ID: {})", 
                    entity.getFullName(), entity.getEntityId());
            }
        }
        
        // Priority 2: LEI matching
        if (extracted.getLei() != null) {
            logger.debug("Searching by LEI: {}", extracted.getLei());
            List<LoanIQEntity> leiMatches = repository.findByLEI(extracted.getLei());
            
            for (LoanIQEntity entity : leiMatches) {
                // Check if already matched by MEI
                boolean alreadyMatched = matches.stream()
                    .anyMatch(m -> m.getMatchedEntity().getEntityId().equals(entity.getEntityId()));
                
                if (!alreadyMatched) {
                    MatchResult result = createIdentifierMatch(entity, "LEI",
                        extracted.getLei(), 35.0);
                    matches.add(result);
                } else {
                    // Enhance existing match
                    enhanceExistingMatch(matches, entity.getEntityId(), "LEI", 20.0);
                }
            }
        }
        
        // Priority 3: EIN matching
        if (extracted.getEin() != null) {
            logger.debug("Searching by EIN: {}", extracted.getEin());
            List<LoanIQEntity> einMatches = repository.findByEIN(extracted.getEin());
            
            for (LoanIQEntity entity : einMatches) {
                boolean alreadyMatched = matches.stream()
                    .anyMatch(m -> m.getMatchedEntity().getEntityId().equals(entity.getEntityId()));
                
                if (!alreadyMatched) {
                    MatchResult result = createIdentifierMatch(entity, "EIN",
                        extracted.getEin(), 30.0);
                    matches.add(result);
                } else {
                    enhanceExistingMatch(matches, entity.getEntityId(), "EIN", 15.0);
                }
            }
        }
        
        // Priority 4: Debt Domain ID matching
        if (extracted.getDebtDomainId() != null) {
            logger.debug("Searching by Debt Domain ID: {}", extracted.getDebtDomainId());
            List<LoanIQEntity> ddMatches = repository.findByDebtDomainId(extracted.getDebtDomainId());
            
            for (LoanIQEntity entity : ddMatches) {
                boolean alreadyMatched = matches.stream()
                    .anyMatch(m -> m.getMatchedEntity().getEntityId().equals(entity.getEntityId()));
                
                if (!alreadyMatched) {
                    MatchResult result = createIdentifierMatch(entity, "Debt Domain ID",
                        extracted.getDebtDomainId(), 25.0);
                    matches.add(result);
                } else {
                    enhanceExistingMatch(matches, entity.getEntityId(), "Debt Domain ID", 10.0);
                }
            }
        }
        
        logger.info("Identifier matching found {} results", matches.size());
        return matches;
    }
    
    /**
     * Create a match result for an identifier match
     */
    private MatchResult createIdentifierMatch(LoanIQEntity entity, String identifierType,
                                             String identifierValue, double baseScore) {
        MatchResult result = new MatchResult();
        result.setMatchedEntity(entity);
        result.setScore(baseScore);
        result.addEvidence(String.format("%s exact match: %s", identifierType, identifierValue));
        result.addScoreComponent(identifierType.toLowerCase().replace(" ", "_") + "_match", baseScore);
        
        // Add location information if relevant
        if (entity.isLocation()) {
            result.addEvidence("Match is a location sub-entity");
        }
        
        return result;
    }
    
    /**
     * Enhance an existing match with additional identifier evidence
     */
    private void enhanceExistingMatch(List<MatchResult> matches, Long entityId,
                                     String identifierType, double boost) {
        matches.stream()
            .filter(m -> m.getMatchedEntity().getEntityId().equals(entityId))
            .findFirst()
            .ifPresent(match -> {
                double newScore = Math.min(100, match.getScore() + boost);
                match.setScore(newScore);
                match.addEvidence("Additional " + identifierType + " match");
                match.addScoreComponent(identifierType.toLowerCase() + "_boost", boost);
                logger.debug("Enhanced match for entity {} with {} (new score: {})",
                    entityId, identifierType, newScore);
            });
    }
}