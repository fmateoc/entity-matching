package com.loantrading.matching.engine;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.MatchResult;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates matches using cross-source data (ADF vs Tax Form) and provides score boosts.
 * This class does NOT create discrepancies; that is the sole responsibility of the DiscrepancyDetector.
 */
public class CrossSourceValidator {
    private static final Logger logger = LoggerFactory.getLogger(CrossSourceValidator.class);
    
    private final JaroWinklerDistance jaroWinkler;
    private final NameNormalizer nameNormalizer;
    
    public CrossSourceValidator() {
        this.jaroWinkler = new JaroWinklerDistance();
        this.nameNormalizer = new NameNormalizer();
    }
    
    /**
     * Validate and enhance match using tax form data.
     * This method only provides positive score boosts for consistent data
     * and does not create discrepancies.
     */
    public void validate(MatchResult match, ExtractedEntity adf, ExtractedEntity taxForm) {
        if (taxForm == null) {
            return;
        }
        
        double boost = 0;
        
        // Boost for consistent EIN between forms.
        if (adf.getEin() != null && taxForm.getEin() != null && adf.getEin().equals(taxForm.getEin())) {
            boost += 10;
            match.addEvidence("EIN consistent between ADF and tax form");
        }

        // Boost for consistent legal name between forms.
        if (adf.getLegalName() != null && taxForm.getLegalName() != null) {
            String normalizedAdfName = nameNormalizer.normalize(adf.getLegalName());
            String normalizedTaxName = nameNormalizer.normalize(taxForm.getLegalName());
            double nameSimilarity = jaroWinkler.apply(normalizedAdfName, normalizedTaxName);
            if (nameSimilarity > 0.95) {
                boost += 8;
                match.addEvidence("Legal name highly consistent across forms");
            }
        }
        
        // Boost for consistent country across forms.
        if (adf.getCountryCode() != null && taxForm.getCountryCode() != null && adf.getCountryCode().equals(taxForm.getCountryCode())) {
            boost += 2;
            match.addEvidence("Country consistent across forms");
        }
        
        // Boost for additional identifiers from tax form that match the DB record.
        boost += validateAdditionalIdentifiers(match, adf, taxForm);
        
        // Apply final boost to match score
        if (boost > 0) {
            double newScore = Math.max(0, Math.min(100, match.getScore() + boost));
            match.setScore(newScore);
            match.addScoreComponent("tax_form_validation", boost);
            
            logger.debug("Cross-source validation adjusted score by {} to {}",
                boost, newScore);
        }
    }
    
    private double validateAdditionalIdentifiers(MatchResult match, ExtractedEntity adf, 
                                                ExtractedEntity taxForm) {
        double boost = 0;
        
        // Check if tax form has additional identifiers not in ADF
        if (taxForm.getLei() != null && adf.getLei() == null) {
            match.addEvidence("Additional LEI from tax form: " + taxForm.getLei());
            
            // Check against LoanIQ
            if (match.getMatchedEntity().getLei() != null &&
                match.getMatchedEntity().getLei().equals(taxForm.getLei())) {
                boost += 15;
                match.addEvidence("Tax form LEI matches LoanIQ");
            }
        }
        
        if (taxForm.getDebtDomainId() != null && adf.getDebtDomainId() == null) {
            match.addEvidence("Additional Debt Domain ID from tax form: " + 
                taxForm.getDebtDomainId());
            
            // Check against LoanIQ
            if (match.getMatchedEntity().getDebtDomainId() != null &&
                match.getMatchedEntity().getDebtDomainId().equals(taxForm.getDebtDomainId())) {
                boost += 10;
                match.addEvidence("Tax form Debt Domain ID matches LoanIQ");
            }
        }
        
        return boost;
    }
}