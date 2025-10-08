package com.loantrading.matching.framework.workflow;

/**
 * Represents a single, executable step in a matching workflow.
 *
 * Each step in the workflow takes the {@link MatchingContext}, performs a specific
 * action (e.g., extracting data, fetching candidates, running a match),
 * and updates the context with its results.
 */
@FunctionalInterface
public interface IWorkflowStep {

    /**
     * Executes the logic for this workflow step.
     *
     * @param context The shared context object for the workflow instance.
     */
    void execute(MatchingContext context);
}