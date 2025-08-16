package com.loantrading.matching.engine;

import com.loantrading.matching.entity.LoanIQEntity;
import com.loantrading.matching.repository.LoanIQRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects potential duplicate entities in LoanIQ
 */
public class DuplicateDetector {
    private static final Logger logger = LoggerFactory.getLogger(DuplicateDetector.class);
    
    private final LoanIQRepository repository;
    
    public DuplicateDetector(LoanIQRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Find potential duplicates for a given entity
     */
    public List<LoanIQEntity> findPotentialDuplicates(LoanIQEntity entity) {
        Set<LoanIQEntity> duplicates = new HashSet<>();
        
        try {
            // Check for same identifiers
            if (entity.getMei() != null) {
                List<LoanIQEntity> meiDupes = repository.findByMEI(entity.getMei());
                for (LoanIQEntity dupe : meiDupes) {
                    if (!dupe.getEntityId().equals(entity.getEntityId())) {
                        duplicates.add(dupe);
                        logger.debug("Found duplicate by MEI: {} (ID: {})",
                            dupe.getFullName(), dupe.getEntityId());
                    }
                }
            }
            
            // Check for same LEI
            if (entity.getLei() != null) {
                List<LoanIQEntity> leiDupes = repository.findByLEI(entity.getLei());
                for (LoanIQEntity dupe : leiDupes) {
                    if (!dupe.getEntityId().equals(entity.getEntityId())) {
                        duplicates.add(dupe);
                        logger.debug("Found duplicate by LEI: {} (ID: {})",
                            dupe.getFullName(), dupe.getEntityId());
                    }
                }
            }
            
            // Check for same EIN
            if (entity.getEin() != null) {
                List<LoanIQEntity> einDupes = repository.findByEIN(entity.getEin());
                for (LoanIQEntity dupe : einDupes) {
                    if (!dupe.getEntityId().equals(entity.getEntityId())) {
                        duplicates.add(dupe);
                        logger.debug("Found duplicate by EIN: {} (ID: {})",
                            dupe.getFullName(), dupe.getEntityId());
                    }
                }
            }
            
            // Check for short name variations (punctuation-only differences)
            if (entity.getShortName() != null) {
                String cleanedShortName = entity.getShortName().replaceAll("[^a-zA-Z0-9]", "");
                List<LoanIQEntity> shortNameDupes = repository.findByCleanedShortName(cleanedShortName);
                
                for (LoanIQEntity dupe : shortNameDupes) {
                    if (!dupe.getEntityId().equals(entity.getEntityId())) {
                        // Additional check: are they really similar?
                        String dupeCleanedName = dupe.getShortName().replaceAll("[^a-zA-Z0-9]", "");
                        if (cleanedShortName.equalsIgnoreCase(dupeCleanedName)) {
                            duplicates.add(dupe);
                            logger.debug("Found duplicate by short name variation: {} vs {} (ID: {})",
                                entity.getShortName(), dupe.getShortName(), dupe.getEntityId());
                        }
                    }
                }
            }
            
            // Check for very similar full names
            if (entity.getFullName() != null) {
                List<LoanIQEntity> nameCandidates = repository.findCandidatesByName(
                    entity.getFullName(), entity.getUltimateParent()
                );
                
                for (LoanIQEntity candidate : nameCandidates) {
                    if (!candidate.getEntityId().equals(entity.getEntityId()) &&
                        !duplicates.contains(candidate)) {
                        // Check if names are very similar
                        if (areNamesSimilar(entity.getFullName(), candidate.getFullName())) {
                            duplicates.add(candidate);
                            logger.debug("Found duplicate by similar name: {} vs {} (ID: {})",
                                entity.getFullName(), candidate.getFullName(), 
                                candidate.getEntityId());
                        }
                    }
                }
            }
            
            logger.info("Found {} potential duplicates for entity {}",
                duplicates.size(), entity.getEntityId());
            
        } catch (Exception e) {
            logger.error("Error detecting duplicates for entity " + entity.getEntityId(), e);
        }
        
        return new ArrayList<>(duplicates);
    }
    
    /**
     * Check if two names are similar enough to be potential duplicates
     */
    private boolean areNamesSimilar(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return false;
        }
        
        // Normalize for comparison
        String norm1 = normalizeName(name1);
        String norm2 = normalizeName(name2);
        
        // Exact match after normalization
        if (norm1.equals(norm2)) {
            return true;
        }
        
        // Check for subset relationship
        if (norm1.contains(norm2) || norm2.contains(norm1)) {
            // One name contains the other - likely duplicate
            return true;
        }
        
        // Check for word reordering
        String[] words1 = norm1.split("\\s+");
        String[] words2 = norm2.split("\\s+");
        
        if (words1.length == words2.length && words1.length > 1) {
            Set<String> set1 = new HashSet<>();
            Set<String> set2 = new HashSet<>();
            
            for (String word : words1) set1.add(word);
            for (String word : words2) set2.add(word);
            
            if (set1.equals(set2)) {
                return true; // Same words, different order
            }
        }
        
        return false;
    }
    
    /**
     * Normalize name for duplicate detection
     */
    private String normalizeName(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", " ")  // Remove special characters
            .replaceAll("\\s+", " ")           // Normalize spaces
            .trim();
    }
}