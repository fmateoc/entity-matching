package com.loantrading.matching.engine;

import com.loantrading.matching.entity.*;
import com.loantrading.matching.repository.LoanIQRepository;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects discrepancies between extracted data, tax forms, and LoanIQ entities
 */
public class DiscrepancyDetector {
    private static final Logger logger = LoggerFactory.getLogger(DiscrepancyDetector.class);
    
    private final LoanIQRepository repository;
    private final JaroWinklerDistance jaroWinkler;
    
    public DiscrepancyDetector(LoanIQRepository repository) {
        this.repository = repository;
        this.jaroWinkler = new JaroWinklerDistance();
    }
    
    /**
     * Detect all discrepancies for a match
     */
    public List<Discrepancy> detect(ExtractedEntity adf, ExtractedEntity taxForm,
                                   LoanIQEntity matched) {
        List<Discrepancy> discrepancies = new ArrayList<>();
        
        // Check identifier discrepancies
        detectIdentifierDiscrepancies(adf, matched, discrepancies);
        
        // Check geographic discrepancies
        detectGeographicDiscrepancies(adf, matched, discrepancies);
        
        // Check name discrepancies
        detectNameDiscrepancies(adf, matched, discrepancies);
        
        // Cross-source validation if tax form available
        if (taxForm != null) {
            detectCrossSourceDiscrepancies(adf, taxForm, discrepancies);
        }
        
        // Check for internal LoanIQ inconsistencies
        detectInternalInconsistencies(matched, discrepancies);
        
        logger.info("Detected {} discrepancies for entity {}",
            discrepancies.size(), matched.getEntityId());
        
        return discrepancies;
    }
    
    private void detectIdentifierDiscrepancies(ExtractedEntity extracted, LoanIQEntity matched,
                                              List<Discrepancy> discrepancies) {
        // MEI discrepancy
        if (extracted.getMei() != null && matched.getMei() != null &&
            !extracted.getMei().equals(matched.getMei())) {
            Discrepancy disc = new Discrepancy(
                "MEI_MISMATCH",
                DiscrepancySeverity.CRITICAL,
                "MEI in form differs from LoanIQ"
            );
            disc.addDetail("form_mei", extracted.getMei());
            disc.addDetail("loaniq_mei", matched.getMei());
            disc.setSource("IDENTIFIER_CHECK");
            discrepancies.add(disc);
        }
        
        // Missing MEI in LoanIQ
        if (extracted.getMei() != null && matched.getMei() == null) {
            Discrepancy disc = new Discrepancy(
                "MEI_MISSING_LOANIQ",
                DiscrepancySeverity.HIGH,
                "MEI present in form but missing in LoanIQ"
            );
            disc.addDetail("form_mei", extracted.getMei());
            disc.setSource("IDENTIFIER_CHECK");
            discrepancies.add(disc);
        }
        
        // LEI discrepancy
        if (extracted.getLei() != null && matched.getLei() != null &&
            !extracted.getLei().equals(matched.getLei())) {
            Discrepancy disc = new Discrepancy(
                "LEI_MISMATCH",
                DiscrepancySeverity.HIGH,
                "LEI in form differs from LoanIQ"
            );
            disc.addDetail("form_lei", extracted.getLei());
            disc.addDetail("loaniq_lei", matched.getLei());
            disc.setSource("IDENTIFIER_CHECK");
            discrepancies.add(disc);
        }
        
        // EIN discrepancy (normalize for comparison)
        if (extracted.getEin() != null && matched.getEin() != null) {
            String normalizedExtractedEIN = extracted.getEin().replaceAll("-", "");
            String normalizedMatchedEIN = matched.getEin().replaceAll("-", "");
            
            if (!normalizedExtractedEIN.equals(normalizedMatchedEIN)) {
                Discrepancy disc = new Discrepancy(
                    "EIN_MISMATCH",
                    DiscrepancySeverity.HIGH,
                    "EIN in form differs from LoanIQ"
                );
                disc.addDetail("form_ein", extracted.getEin());
                disc.addDetail("loaniq_ein", matched.getEin());
                disc.setSource("IDENTIFIER_CHECK");
                discrepancies.add(disc);
            }
        }
        
        // Debt Domain ID discrepancy
        if (extracted.getDebtDomainId() != null && matched.getDebtDomainId() != null &&
            !extracted.getDebtDomainId().equals(matched.getDebtDomainId())) {
            Discrepancy disc = new Discrepancy(
                "DEBT_DOMAIN_ID_MISMATCH",
                DiscrepancySeverity.MEDIUM,
                "Debt Domain ID in form differs from LoanIQ"
            );
            disc.addDetail("form_dd_id", extracted.getDebtDomainId());
            disc.addDetail("loaniq_dd_id", matched.getDebtDomainId());
            disc.setSource("IDENTIFIER_CHECK");
            discrepancies.add(disc);
        }
    }
    
    private void detectGeographicDiscrepancies(ExtractedEntity extracted, LoanIQEntity matched,
                                              List<Discrepancy> discrepancies) {
        // MEI country code vs address country
        if (extracted.getMei() != null && extracted.getCountryCode() != null) {
            String meiCountry = extracted.getMei().substring(0, 2);
            if (!meiCountry.equals(extracted.getCountryCode())) {
                Discrepancy disc = new Discrepancy(
                    "COUNTRY_MISMATCH_MEI_ADDRESS",
                    DiscrepancySeverity.MEDIUM,
                    "MEI country code doesn't match address country"
                );
                disc.addDetail("mei_country", meiCountry);
                disc.addDetail("address_country", extracted.getCountryCode());
                disc.setSource("GEOGRAPHIC_CHECK");
                discrepancies.add(disc);
            }
        }
        
        // Form vs LoanIQ country
        if (extracted.getCountryCode() != null && matched.getCountryCode() != null &&
            !extracted.getCountryCode().equals(matched.getCountryCode())) {
            Discrepancy disc = new Discrepancy(
                "COUNTRY_MISMATCH_FORM_LOANIQ",
                DiscrepancySeverity.MEDIUM,
                "Form country differs from LoanIQ country"
            );
            disc.addDetail("form_country", extracted.getCountryCode());
            disc.addDetail("loaniq_country", matched.getCountryCode());
            disc.setSource("GEOGRAPHIC_CHECK");
            discrepancies.add(disc);
        }
        
        // Tax vs legal address country
        if (extracted.getTaxCountryCode() != null && extracted.getCountryCode() != null &&
            !extracted.getTaxCountryCode().equals(extracted.getCountryCode())) {
            Discrepancy disc = new Discrepancy(
                "COUNTRY_MISMATCH_TAX_LEGAL",
                DiscrepancySeverity.LOW,
                "Tax country differs from legal address country"
            );
            disc.addDetail("tax_country", extracted.getTaxCountryCode());
            disc.addDetail("legal_country", extracted.getCountryCode());
            disc.setSource("GEOGRAPHIC_CHECK");
            discrepancies.add(disc);
        }
    }
    
    private void detectNameDiscrepancies(ExtractedEntity extracted, LoanIQEntity matched,
                                        List<Discrepancy> discrepancies) {
        // DBA handling
        if (extracted.getDba() != null && 
            !matched.getFullName().toUpperCase().contains("DBA") &&
            !matched.getFullName().toUpperCase().contains("D/B/A")) {
            Discrepancy disc = new Discrepancy(
                "DBA_NOT_IN_LOANIQ",
                DiscrepancySeverity.LOW,
                "Form contains DBA but LoanIQ doesn't"
            );
            disc.addDetail("form_dba", extracted.getDba());
            disc.setSource("NAME_CHECK");
            discrepancies.add(disc);
        }
        
        // Fund manager mismatch for composite entities
        if (extracted.getFundManager() != null && matched.getUltimateParent() != null) {
            double similarity = jaroWinkler.apply(
                extracted.getFundManager(), matched.getUltimateParent()
            );
            if (similarity < 0.7) {
                Discrepancy disc = new Discrepancy(
                    "FUND_MANAGER_MISMATCH",
                    DiscrepancySeverity.MEDIUM,
                    "Fund manager name differs significantly"
                );
                disc.addDetail("form_manager", extracted.getFundManager());
                disc.addDetail("loaniq_manager", matched.getUltimateParent());
                disc.addDetail("similarity", similarity);
                disc.setSource("NAME_CHECK");
                discrepancies.add(disc);
            }
        }
        
        // Missing fund manager
        if (extracted.getFundManager() != null && matched.getUltimateParent() == null) {
            Discrepancy disc = new Discrepancy(
                "FUND_MANAGER_MISSING_LOANIQ",
                DiscrepancySeverity.MEDIUM,
                "Fund manager in form but not in LoanIQ"
            );
            disc.addDetail("form_manager", extracted.getFundManager());
            disc.setSource("NAME_CHECK");
            discrepancies.add(disc);
        }
        
        // Unexpected fund manager in LoanIQ
        if (extracted.getFundManager() == null && matched.getUltimateParent() != null) {
            Discrepancy disc = new Discrepancy(
                "UNEXPECTED_FUND_MANAGER_LOANIQ",
                DiscrepancySeverity.MEDIUM,
                "LoanIQ has fund manager but form doesn't"
            );
            disc.addDetail("loaniq_manager", matched.getUltimateParent());
            disc.setSource("NAME_CHECK");
            discrepancies.add(disc);
        }
    }
    
    private void detectCrossSourceDiscrepancies(ExtractedEntity adf, ExtractedEntity taxForm,
                                               List<Discrepancy> discrepancies) {
        // EIN mismatch between forms
        if (adf.getEin() != null && taxForm.getEin() != null &&
            !adf.getEin().equals(taxForm.getEin())) {
            Discrepancy disc = new Discrepancy(
                "EIN_MISMATCH_CROSS_FORM",
                DiscrepancySeverity.CRITICAL,
                "EIN differs between ADF and tax form"
            );
            disc.addDetail("adf_ein", adf.getEin());
            disc.addDetail("tax_form_ein", taxForm.getEin());
            disc.setSource("CROSS_SOURCE_CHECK");
            discrepancies.add(disc);
        }
        
        // Legal name mismatch
        if (adf.getLegalName() != null && taxForm.getLegalName() != null) {
            double similarity = jaroWinkler.apply(
                adf.getLegalName(), taxForm.getLegalName()
            );
            if (similarity < 0.85) {
                Discrepancy disc = new Discrepancy(
                    "LEGAL_NAME_MISMATCH_CROSS_FORM",
                    DiscrepancySeverity.HIGH,
                    "Legal name differs significantly between forms"
                );
                disc.addDetail("adf_name", adf.getLegalName());
                disc.addDetail("tax_form_name", taxForm.getLegalName());
                disc.addDetail("similarity", similarity);
                disc.setSource("CROSS_SOURCE_CHECK");
                discrepancies.add(disc);
            }
        }
        
        // Country mismatch
        if (adf.getCountryCode() != null && taxForm.getCountryCode() != null &&
            !adf.getCountryCode().equals(taxForm.getCountryCode())) {
            Discrepancy disc = new Discrepancy(
                "COUNTRY_MISMATCH_CROSS_FORM",
                DiscrepancySeverity.MEDIUM,
                "Country differs between ADF and tax form"
            );
            disc.addDetail("adf_country", adf.getCountryCode());
            disc.addDetail("tax_form_country", taxForm.getCountryCode());
            disc.setSource("CROSS_SOURCE_CHECK");
            discrepancies.add(disc);
        }
        
        // MEI mismatch
        if (adf.getMei() != null && taxForm.getMei() != null &&
            !adf.getMei().equals(taxForm.getMei())) {
            Discrepancy disc = new Discrepancy(
                "MEI_MISMATCH_CROSS_FORM",
                DiscrepancySeverity.CRITICAL,
                "MEI differs between ADF and tax form"
            );
            disc.addDetail("adf_mei", adf.getMei());
            disc.addDetail("tax_form_mei", taxForm.getMei());
            disc.setSource("CROSS_SOURCE_CHECK");
            discrepancies.add(disc);
        }
    }
    
    private void detectInternalInconsistencies(LoanIQEntity entity,
                                              List<Discrepancy> discrepancies) {
        // Check if short name contains suspicious patterns
        if (entity.getShortName() != null) {
            String cleanedShortName = entity.getShortName().replaceAll("[^a-zA-Z0-9]", "");
            
            // Check for potential duplicates
            List<LoanIQEntity> similarEntities = repository.findByCleanedShortName(cleanedShortName);
            if (similarEntities.size() > 1) {
                Discrepancy disc = new Discrepancy(
                    "POTENTIAL_DUPLICATE_SHORT_NAME",
                    DiscrepancySeverity.LOW,
                    "Multiple entities with similar short names detected"
                );
                disc.addDetail("short_name", entity.getShortName());
                disc.addDetail("similar_count", similarEntities.size());
                disc.setSource("INTERNAL_CHECK");
                discrepancies.add(disc);
            }
        }
        
        // Check if location record without parent
        if (entity.isLocation() && entity.getParentCustomerId() == null) {
            Discrepancy disc = new Discrepancy(
                "ORPHANED_LOCATION_RECORD",
                DiscrepancySeverity.MEDIUM,
                "Location record without parent customer"
            );
            disc.addDetail("entity_id", entity.getEntityId());
            disc.setSource("INTERNAL_CHECK");
            discrepancies.add(disc);
        }
        
        // Check for MEI country code consistency
        if (entity.getMei() != null && entity.getCountryCode() != null) {
            String meiCountry = entity.getMei().substring(0, 2);
            if (!meiCountry.equals(entity.getCountryCode())) {
                Discrepancy disc = new Discrepancy(
                    "INTERNAL_COUNTRY_MISMATCH",
                    DiscrepancySeverity.MEDIUM,
                    "LoanIQ MEI country doesn't match stored country code"
                );
                disc.addDetail("mei_country", meiCountry);
                disc.addDetail("stored_country", entity.getCountryCode());
                disc.setSource("INTERNAL_CHECK");
                discrepancies.add(disc);
            }
        }
    }
}