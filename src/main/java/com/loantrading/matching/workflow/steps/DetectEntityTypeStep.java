package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.detection.EntityTypeDetector;
import com.loantrading.matching.entity.EntityType;
import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.ProcessingResult;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;

/**
 * Workflow step to detect the entity type based on the extracted ADF data.
 */
public class DetectEntityTypeStep implements IWorkflowStep {

    private final EntityTypeDetector typeDetector;

    public DetectEntityTypeStep(EntityTypeDetector typeDetector) {
        this.typeDetector = typeDetector;
    }

    @Override
    public void execute(MatchingContext context) {
        ProcessingResult result = context.getRequired(ContextKeys.PROCESSING_RESULT, ProcessingResult.class);
        ExtractedEntity adfData = context.getRequired(ContextKeys.EXTRACTED_ADF_DATA, ExtractedEntity.class);

        EntityType entityType = typeDetector.detectType(adfData);
        result.setEntityType(entityType);
        result.addAuditEntry("Entity type detected: " + entityType);
        result.addMetadata("entity_type", entityType.toString());

        context.put(ContextKeys.ENTITY_TYPE, entityType);
    }
}