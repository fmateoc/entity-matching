package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.ProcessingResult;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Workflow step to first validate the document pair (if both exist) and then
 * consolidate the data into a single "master entity" based on the source-of-truth hierarchy.
 */
public class ConsolidateAndValidateDataStep implements IWorkflowStep {

    private static final Logger logger = LoggerFactory.getLogger(ConsolidateAndValidateDataStep.class);

    @Override
    public void execute(MatchingContext context) {
        ProcessingResult result = context.getRequired(ContextKeys.PROCESSING_RESULT, ProcessingResult.class);
        ExtractedEntity adfData = context.getRequired(ContextKeys.EXTRACTED_ADF_DATA, ExtractedEntity.class);
        Optional<ExtractedEntity> taxFormDataOpt = context.get(ContextKeys.EXTRACTED_TAX_FORM_DATA, ExtractedEntity.class);

        // 1. Pre-validation
        if (taxFormDataOpt.isPresent()) {
            if (!preValidateDocumentPair(adfData, taxFormDataOpt.get(), result)) {
                // Halt further processing by not setting a master entity.
                // A subsequent step should check for the presence of the master entity.
                // For now, we'll rely on the ERROR decision.
                return;
            }
        }

        // 2. Consolidation
        ExtractedEntity masterEntity = createMasterEntity(adfData, taxFormDataOpt.orElse(null));
        result.addAuditEntry("Consolidated entity data based on source of truth hierarchy.");
        context.put(ContextKeys.MASTER_ENTITY, masterEntity);
    }

    private boolean preValidateDocumentPair(ExtractedEntity adfData, ExtractedEntity taxFormData, ProcessingResult result) {
        if (adfData == null || taxFormData == null) {
            return true; // Nothing to compare
        }

        String adfEin = adfData.getEin();
        String taxEin = taxFormData.getEin();

        if (adfEin != null && taxEin != null && !adfEin.equals(taxEin)) {
            String errorMessage = String.format("Critical Error: EIN mismatch between documents. ADF EIN: %s, Tax Form EIN: %s. Halting processing.", adfEin, taxEin);
            logger.error(errorMessage);
            result.setDecision("ERROR");
            result.addAuditEntry(errorMessage);
            result.addMetadata("pre-validation-error", "ein_mismatch");
            return false;
        }

        result.addAuditEntry("Pre-validation successful: Documents appear to be for the same entity.");
        return true;
    }

    private ExtractedEntity createMasterEntity(ExtractedEntity adfData, ExtractedEntity taxFormData) {
        if (taxFormData == null) {
            return adfData; // No tax form, so ADF is the master.
        }

        ExtractedEntity masterEntity = new ExtractedEntity();

        // Tax form is the source of truth for Legal Name and EIN.
        masterEntity.setLegalName(taxFormData.getLegalName() != null ? taxFormData.getLegalName() : adfData.getLegalName());
        masterEntity.setEin(taxFormData.getEin() != null ? taxFormData.getEin() : adfData.getEin());

        // ADF is the source of truth for all other fields.
        masterEntity.setFundManager(adfData.getFundManager());
        masterEntity.setMei(adfData.getMei());
        masterEntity.setLei(adfData.getLei());
        masterEntity.setDebtDomainId(adfData.getDebtDomainId());
        masterEntity.setCountryCode(adfData.getCountryCode());
        masterEntity.setContactEmails(adfData.getContactEmails());
        masterEntity.setEmailDomain(adfData.getEmailDomain());

        logger.info("Created master entity. Legal Name: '{}', EIN: '{}' (from Tax Form)", masterEntity.getLegalName(), masterEntity.getEin());

        return masterEntity;
    }
}