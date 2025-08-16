package com.loantrading.matching.engine;

import com.loantrading.matching.entity.*;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates matches using cross-source data (ADF vs Tax Form)
 */
public class CrossSourceValidator {
    private static final Logger logger = LoggerFactory.getLogger(CrossSourceValidator.class);
    
    private final JaroWinklerDistance jaroWinkler;
    
    public CrossSourceValidator() {
        this.jaroWinkler = new JaroWinklerDistance();
    }
    
    /**
     * Validate and enhance match using tax form data
     */
    public void validate(MatchResult match, ExtractedEntity adf, ExtractedEntity taxForm) {
        if (taxForm == null) {
            return;
        }
        
        double boost = 0;
        
        // Check EIN consistency
        boost += validateEIN(match, adf, taxForm);
        
        // Check legal name consistency
        boost += validateLegalName(match, adf, taxForm);
        
        // Check country consistency
        boost += validateCountry(match, adf, taxForm);
        
        // Check for additional identifiers in tax form
        boost += validateAdditionalIdentifiers(match, adf, taxForm);
        
        // Apply boost to match score
        if (boost != 0) {
            double newScore = Math.max(0, Math.min(100, match.getScore() + boost));
            match.setScore(newScore);
            match.addScoreComponent("tax_form_validation", boost);
            
            logger.debug("Cross-source validation adjusted score by {} to {}",
                boost, newScore);
        }
    }
    
    private double validateEIN(MatchResult match, ExtractedEntity adf, ExtractedEntity taxForm) {
        double boost = 0;
        
        if (taxForm.getEin() != null && adf.getEin() != null) {
            if (taxForm.getEin().equals(adf.getEin())) {
                boost += 10;
                match.addEvidence("EIN consistent between ADF and tax form");
                logger.debug("EIN match between forms: {}", taxForm.getEin());
            } else {
                boost -= 15;
                match.addDiscrepancy(new Discrepancy(
                    "EIN_MISMATCH_FORMS",
                    DiscrepancySeverity.CRITICAL,
                    String.format("EIN differs: ADF=%s, Tax=%s", adf.getEin(), taxForm.getEin())
                ));
                logger.warn("EIN mismatch: ADF={}, Tax={}", adf.getEin(), taxForm.getEin());
            }
        } else if (taxForm.getEin() != null && adf.getEin() == null) {
            // Use tax form EIN as more reliable
            boost += 5;
            match.addEvidence("EIN from tax form used for validation: " + taxForm.getEin());
            
            // Check if tax form EIN matches LoanIQ entity
            if (match.getMatchedEntity().getEin() != null) {
                if (match.getMatchedEntity().getEin().equals(taxForm.getEin())) {
                    boost += 10;
                    match.addEvidence("Tax form EIN matches LoanIQ");
                } else {
                    boost -= 10;
                    match.addDiscrepancy(new Discrepancy(
                        "EIN_MISMATCH_TAX_LOANIQ",
                        DiscrepancySeverity.HIGH,
                        "Tax form EIN doesn't match LoanIQ"
                    ));
                }
            }
        }
        
        return boost;
    }
    
    private double validateLegalName(MatchResult match, ExtractedEntity adf, ExtractedEntity taxForm) {
        double boost = 0;
        
        if (taxForm.getLegalName() != null && adf.getLegalName() != null) {
            double nameSimilarity = jaroWinkler.apply(
                taxForm.getLegalName(), adf.getLegalName()
            );
            
            if (nameSimilarity > 0.9) {
                boost += 8;
                match.addEvidence("Legal name highly consistent across forms");
            } else if (nameSimilarity > 0.8) {
                boost += 3;
                match.addEvidence("Legal name consistent across forms");
            } else if (nameSimilarity < 0.7) {
                boost -= 10;
                match.addDiscrepancy(new Discrepancy(
                    "LEGAL_NAME_MISMATCH_FORMS",
                    DiscrepancySeverity.HIGH,
                    String.format("Legal name similarity only %.2f", nameSimilarity)
                ));
                logger.warn("Legal name mismatch between forms: similarity={}", nameSimilarity);
            }
            
            // Also check against LoanIQ
            if (match.getMatchedEntity().getFullName() != null) {
                double loaniqSimilarity = jaroWinkler.apply(
                    taxForm.getLegalName(),
                    match.getMatchedEntity().getFullName()
                );
                
                if (loaniqSimilarity > 0.85) {
                    boost += 5;
                    match.addEvidence("Tax form name matches LoanIQ");
                }
            }
        }
        
        return boost;
    }
    
    private double validateCountry(MatchResult match, ExtractedEntity adf, ExtractedEntity taxForm) {
        double boost = 0;
        
        if (taxForm.getCountryCode() != null && adf.getCountryCode() != null) {
            if (!taxForm.getCountryCode().equals(adf.getCountryCode())) {
                match.addDiscrepancy(new Discrepancy(
                    "COUNTRY_MISMATCH_FORMS",
                    DiscrepancySeverity.MEDIUM,
                    String.format("Country differs: ADF=%s, Tax=%s",
                        adf.getCountryCode(), taxForm.getCountryCode())
                ));
                boost -= 5;
            } else {
                boost += 2;
                match.addEvidence("Country consistent across forms");
            }
        }
        
        // Check tax country if different from legal country
        if (taxForm.getTaxCountryCode() != null && 
            !taxForm.getTaxCountryCode().equals(taxForm.getCountryCode())) {
            match.addEvidence("Tax country differs from legal country: " + 
                taxForm.getTaxCountryCode());
        }
        
        return boost;
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