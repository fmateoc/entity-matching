package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.entity.EntityType;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * A workflow step that routes the matching process to the appropriate
 * sub-flow or strategy based on the detected entity type.
 */
public class RouteMatchingStep implements IWorkflowStep {

    private static final Logger logger = LoggerFactory.getLogger(RouteMatchingStep.class);

    private final IWorkflowStep hierarchicalMatchingStep;
    private final IWorkflowStep standaloneMatchingStep;

    public RouteMatchingStep(IWorkflowStep hierarchicalMatchingStep, IWorkflowStep standaloneMatchingStep) {
        this.hierarchicalMatchingStep = hierarchicalMatchingStep;
        this.standaloneMatchingStep = standaloneMatchingStep;
    }

    @Override
    public void execute(MatchingContext context) {
        Optional<EntityType> entityTypeOpt = context.get(ContextKeys.ENTITY_TYPE, EntityType.class);

        if (entityTypeOpt.isEmpty()) {
            logger.warn("Entity type not found in context, skipping matching.");
            return;
        }

        EntityType entityType = entityTypeOpt.get();
        logger.info("Routing matching based on entity type: {}", entityType);

        if (entityType == EntityType.MANAGED_FUND) {
            hierarchicalMatchingStep.execute(context);
        } else {
            standaloneMatchingStep.execute(context);
        }
    }
}