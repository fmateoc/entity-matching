package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.discrepancy.DiscrepancyDetector;
import com.loantrading.matching.entity.Discrepancy;
import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.MatchedEntity;
import com.loantrading.matching.entity.MatchResult;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;

import java.util.List;
import java.util.Optional;

/**
 * A workflow step that detects discrepancies between the source data and the best matched entity.
 */
public class DetectDiscrepanciesStep implements IWorkflowStep {

    private final DiscrepancyDetector discrepancyDetector;

    public DetectDiscrepanciesStep(DiscrepancyDetector discrepancyDetector) {
        this.discrepancyDetector = discrepancyDetector;
    }

    @Override
    public void execute(MatchingContext context) {
        Optional<ExtractedEntity> adfDataOpt = context.get(ContextKeys.EXTRACTED_ADF_DATA, ExtractedEntity.class);
        Optional<ExtractedEntity> taxFormDataOpt = context.get(ContextKeys.EXTRACTED_TAX_FORM_DATA, ExtractedEntity.class);
        Optional<MatchResult> bestMatchOpt = context.get(ContextKeys.BEST_MATCH, MatchResult.class);

        if (bestMatchOpt.isEmpty()) {
            return; // No match, so no discrepancies to detect.
        }
        MatchResult bestMatch = bestMatchOpt.get();

        // 1. Cross-document discrepancy detection
        if (adfDataOpt.isPresent() && taxFormDataOpt.isPresent()) {
            List<Discrepancy> crossDocDiscrepancies = discrepancyDetector.detectCrossDocumentDiscrepancies(adfDataOpt.get(), taxFormDataOpt.get());
            crossDocDiscrepancies.forEach(bestMatch::addDiscrepancy);
        }

        // 2. Discrepancy detection between consolidated entity and matched DB entity
        Optional<ExtractedEntity> masterEntityOpt = context.get(ContextKeys.MASTER_ENTITY, ExtractedEntity.class);
        if (masterEntityOpt.isPresent()) {
            List<Discrepancy> discrepancies = discrepancyDetector.detect(masterEntityOpt.get(), (MatchedEntity) bestMatch.getMatchedEntity());
            discrepancies.forEach(bestMatch::addDiscrepancy);
        }
    }
}