package com.loantrading.matching.framework.workflow;

import java.util.List;

/**
 * A concrete implementation of {@link IWorkflow} that executes a list of
 * workflow steps in sequential order.
 */
public class SequentialWorkflow implements IWorkflow {

    private final List<IWorkflowStep> steps;

    /**
     * Constructs a new SequentialWorkflow.
     *
     * @param steps The list of workflow steps to be executed in order.
     */
    public SequentialWorkflow(List<IWorkflowStep> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Workflow steps cannot be null or empty.");
        }
        this.steps = steps;
    }

    @Override
    public MatchingContext execute(MatchingContext initialContext) {
        for (IWorkflowStep step : steps) {
            step.execute(initialContext);
        }
        return initialContext;
    }
}