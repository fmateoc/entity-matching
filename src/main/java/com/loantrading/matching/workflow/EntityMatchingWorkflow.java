package com.loantrading.matching.workflow;

import com.loantrading.matching.detection.EntityTypeDetector;
import com.loantrading.matching.extraction.MultiFormatDocumentExtractor;
import com.loantrading.matching.framework.match.IMatchingStrategy;
import com.loantrading.matching.framework.persistence.DbMatchRepository;
import com.loantrading.matching.framework.persistence.IMatchRepository;
import com.loantrading.matching.framework.workflow.IWorkflow;
import com.loantrading.matching.framework.workflow.MatchingContext;
import com.loantrading.matching.framework.workflow.SequentialWorkflow;
import com.loantrading.matching.repository.EntityCandidateProvider;
import com.loantrading.matching.workflow.steps.*;
import com.loantrading.matching.workflow.strategies.ChainedMatchingStrategy;
import com.loantrading.matching.workflow.strategies.FuzzyNameMatchStrategy;
import com.loantrading.matching.workflow.strategies.IdentifierMatchStrategy;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Constructs and executes the specific workflow for the entity matching process.
 * This class assembles all the individual components into a cohesive workflow.
 */
public class EntityMatchingWorkflow {

    private final IWorkflow workflow;

    public EntityMatchingWorkflow(Connection dbConnection, ExecutorService executorService) {
        // --- Component Setup ---
        MultiFormatDocumentExtractor extractor = new MultiFormatDocumentExtractor();
        EntityTypeDetector typeDetector = new EntityTypeDetector();
        IMatchRepository matchRepository = new DbMatchRepository(dbConnection);
        EntityCandidateProvider candidateProvider = new EntityCandidateProvider(dbConnection);

        // --- Strategy Definition ---
        // Define the atomic strategies that can be chained together.
        var identifierStrategy = new IdentifierMatchStrategy();
        var fuzzyNameStrategy = new FuzzyNameMatchStrategy();

        // Create a standard strategy chain: try exact identifiers first, then fall back to fuzzy name matching.
        IMatchingStrategy standardStrategyChain = new ChainedMatchingStrategy(
                candidateProvider,
                List.of(identifierStrategy, fuzzyNameStrategy)
        );

        // --- Workflow Step Definition ---
        // For hierarchical matching, the parent and child strategies use the same standard chain.
        // In a more complex scenario, they could be configured with different chains.
        var hierarchicalStep = new HierarchicalMatchingStep(
                matchRepository,
                standardStrategyChain, // Strategy for the parent (Fund Manager)
                standardStrategyChain, // Strategy for the child (Managed Fund)
                "FUND_MANAGER",
                99.0 // Only persist perfect (identifier) matches
        );

        // For standalone entities, we wrap the standard strategy chain in a simple step.
        var standaloneStep = new StandaloneMatchingStep(standardStrategyChain);

        // The routing step directs the flow based on entity type.
        var routingStep = new RouteMatchingStep(hierarchicalStep, standaloneStep);

        // --- Workflow Assembly ---
        // Assemble the full sequence of steps for the end-to-end process.
        this.workflow = new SequentialWorkflow(List.of(
                new InitializeProcessingStep(),
                new ExtractAdfDataStep(extractor),
                new ExtractTaxFormDataStep(extractor, executorService),
                new DetectEntityTypeStep(typeDetector),
                new ConsolidateAndValidateDataStep(),
                routingStep, // This single step handles all matching logic.
                new SelectBestMatchStep(),
                new DetectDiscrepanciesStep(new com.loantrading.matching.discrepancy.DiscrepancyDetector()),
                new ApplyDiscrepancyPenaltiesStep(),
                new DetermineFinalDecisionStep(),
                new FinalizeProcessingStep()
        ));
    }

    /**
     * Executes the entity matching workflow for a single document pair.
     *
     * @param adfContent The byte content of the Administrative Details Form.
     * @param adfFilename The filename of the ADF.
     * @param taxFormContent The byte content of the Tax Form (can be null).
     * @param taxFormFilename The filename of the Tax Form (can be null).
     * @return The final, populated MatchingContext containing the results.
     */
    public MatchingContext run(byte[] adfContent, String adfFilename, byte[] taxFormContent, String taxFormFilename) {
        MatchingContext context = new MatchingContext();
        context.put(ContextKeys.ADF_CONTENT, adfContent);
        context.put(ContextKeys.ADF_FILENAME, adfFilename);

        if (taxFormContent != null) {
            context.put(ContextKeys.TAX_FORM_CONTENT, taxFormContent);
            context.put(ContextKeys.TAX_FORM_FILENAME, taxFormFilename);
        }

        return workflow.execute(context);
    }
}