package com.loantrading.matching.engine;

import com.loantrading.matching.entity.*;
import com.loantrading.matching.repository.LoanIQRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main matching engine that coordinates all matching strategies
 */
public class MatchingEngine {
    private static final Logger logger = LoggerFactory.getLogger(MatchingEngine.class);
    
    private final LoanIQRepository repository;
    private final IdentifierMatcher identifierMatcher;
    private final FuzzyNameMatcher fuzzyNameMatcher;
    private final EmailDomainMatcher emailDomainMatcher;
    private final DiscrepancyDetector discrepancyDetector;
    private final ConfidenceScorer confidenceScorer;
    private final DuplicateDetector duplicateDetector;
    private final CrossSourceValidator crossSourceValidator;
    
    public MatchingEngine(Connection dbConnection) {
        this.repository = new LoanIQRepository(dbConnection);
        this.identifierMatcher = new IdentifierMatcher(repository);
        this.fuzzyNameMatcher = new FuzzyNameMatcher();
        this.emailDomainMatcher = new EmailDomainMatcher();
        this.discrepancyDetector = new DiscrepancyDetector(repository);
        this.confidenceScorer = new ConfidenceScorer();
        this.duplicateDetector = new DuplicateDetector(repository);
        this.crossSourceValidator = new CrossSourceValidator();
    }
    
    /**
     * Find matches for an extracted entity
     */
    public List<MatchResult> findMatches(ExtractedEntity extracted, ExtractedEntity taxForm) {
        List<MatchResult> allMatches = new ArrayList<>();
        Set<Long> processedEntityIds = new HashSet<>();
        
        try {
            logger.info("Starting matching process for entity: {}", 
                extracted.getLegalName() != null ? extracted.getLegalName() : "Unknown");
            
            // Step 1: Try identifier-based matching (highest priority)
            List<MatchResult> identifierMatches = identifierMatcher.match(extracted);
            for (MatchResult match : identifierMatches) {
                if (!processedEntityIds.contains(match.getMatchedEntity().getEntityId())) {
                    allMatches.add(match);
                    processedEntityIds.add(match.getMatchedEntity().getEntityId());
                    match.setMatchStrategy("IDENTIFIER");
                    logger.debug("Added identifier match: {} (score: {})", 
                        match.getMatchedEntity().getFullName(), match.getScore());
                }
            }
            
            // Step 2: Try fuzzy name matching if needed
            if (allMatches.size() < 5) {
                List<LoanIQEntity> candidates = repository.findCandidatesByName(
                    extracted.getLegalName(), extracted.getFundManager()
                );
                
                logger.debug("Found {} name-based candidates", candidates.size());
                
                for (LoanIQEntity candidate : candidates) {
                    if (!processedEntityIds.contains(candidate.getEntityId())) {
                        MatchResult fuzzyMatch = fuzzyNameMatcher.match(extracted, candidate);
                        if (fuzzyMatch.getScore() > 50) {
                            fuzzyMatch.setMatchStrategy("FUZZY_NAME");
                            allMatches.add(fuzzyMatch);
                            processedEntityIds.add(candidate.getEntityId());
                            logger.debug("Added fuzzy match: {} (score: {})",
                                candidate.getFullName(), fuzzyMatch.getScore());
                        }
                    }
                }
            }
            
            // Step 3: Enhance with email domain evidence
            if (extracted.getEmailDomain() != null) {
                logger.debug("Enhancing matches with email domain: {}", extracted.getEmailDomain());
                
                for (MatchResult match : allMatches) {
                    emailDomainMatcher.enhance(match, extracted.getEmailDomain());
                }
                
                // Also try email-based matching if few results
                if (allMatches.size() < 3) {
                    List<LoanIQEntity> emailCandidates = repository.findByEmailDomain(
                        extracted.getEmailDomain()
                    );
                    
                    for (LoanIQEntity candidate : emailCandidates) {
                        if (!processedEntityIds.contains(candidate.getEntityId())) {
                            MatchResult emailMatch = new MatchResult();
                            emailMatch.setMatchedEntity(candidate);
                            emailMatch.setScore(60);
                            emailMatch.setMatchStrategy("EMAIL_DOMAIN");
                            emailMatch.addEvidence("Email domain match: " + extracted.getEmailDomain());
                            allMatches.add(emailMatch);
                            processedEntityIds.add(candidate.getEntityId());
                        }
                    }
                }
            }
            
            // Step 4: Cross-validate with tax form if available
            if (taxForm != null) {
                logger.debug("Cross-validating with tax form data");
                for (MatchResult match : allMatches) {
                    crossSourceValidator.validate(match, extracted, taxForm);
                }
            }
            
            // Step 5: Detect discrepancies for each match
            for (MatchResult match : allMatches) {
                List<Discrepancy> discrepancies = discrepancyDetector.detect(
                    extracted, taxForm, match.getMatchedEntity()
                );
                match.getDiscrepancies().addAll(discrepancies);
                
                // Check for duplicates
                List<LoanIQEntity> duplicates = duplicateDetector.findPotentialDuplicates(
                    match.getMatchedEntity()
                );
                match.getPotentialDuplicates().addAll(duplicates);
                
                if (!duplicates.isEmpty()) {
                    logger.warn("Found {} potential duplicates for entity {}",
                        duplicates.size(), match.getMatchedEntity().getEntityId());
                }
            }
            
            // Step 6: Calculate final confidence scores
            for (MatchResult match : allMatches) {
                confidenceScorer.calculateFinalScore(match, extracted);
            }
            
            // Sort by score descending
            allMatches.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            
            logger.info("Found {} potential matches, returning top 5", allMatches.size());
            
        } catch (Exception e) {
            logger.error("Error during matching process", e);
        }
        
        // Return top 5 matches
        return allMatches.stream()
            .limit(5)
            .collect(Collectors.toList());
    }
    
    /**
     * Close resources
     */
    public void close() {
        repository.close();
    }
}