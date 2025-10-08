package com.loantrading.matching.framework.workflow;

/**
 * Represents a complete, end-to-end workflow composed of multiple steps.
 *
 * A workflow is responsible for executing a series of {@link IWorkflowStep}
 * implementations in a defined order to achieve a business goal, such as
 * matching an entity or processing a batch of documents.
 */
public interface IWorkflow {

    /**
     * Executes the workflow.
     *
     * @param initialContext The initial state and input data for the workflow.
     * @return The final context after all steps have been executed, containing the results.
     */
    MatchingContext execute(MatchingContext initialContext);
}