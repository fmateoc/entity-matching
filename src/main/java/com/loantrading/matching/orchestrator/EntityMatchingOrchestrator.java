package com.loantrading.matching.orchestrator;

import com.loantrading.matching.entity.ProcessingResult;
import com.loantrading.matching.framework.workflow.MatchingContext;
import com.loantrading.matching.workflow.EntityMatchingWorkflow;
import com.loantrading.matching.workflow.steps.ContextKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Main orchestrator for the entity matching process.
 * This class is the main entry point for processing documents. It has been refactored
 * to delegate the complex processing logic to a composable, workflow-based system.
 */
public class EntityMatchingOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(EntityMatchingOrchestrator.class);

    private final ExecutorService executorService;
    private final Connection dbConnection; // Keep connection for batch processing

    public EntityMatchingOrchestrator(Connection dbConnection) {
        this.dbConnection = dbConnection;
        this.executorService = Executors.newFixedThreadPool(4);
    }

    /**
     * Process a single pair of documents and return the matching result.
     * The core logic is now handled by the EntityMatchingWorkflow.
     */
    public ProcessingResult processDocuments(byte[] adfContent, String adfFilename,
                                             byte[] taxFormContent, String taxFormFilename) {
        try {
            // The workflow requires its own executor service instance for its steps.
            EntityMatchingWorkflow workflow = new EntityMatchingWorkflow(dbConnection, executorService);
            MatchingContext finalContext = workflow.run(adfContent, adfFilename, taxFormContent, taxFormFilename);
            return finalContext.getRequired(ContextKeys.PROCESSING_RESULT, ProcessingResult.class);
        } catch (Exception e) {
            logger.error("Processing failed at the orchestrator level", e);
            ProcessingResult errorResult = new ProcessingResult();
            errorResult.addAuditEntry("Orchestrator-level failure: " + e.getMessage());
            errorResult.setDecision("ERROR");
            errorResult.addMetadata("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * Process a batch of documents concurrently.
     */
    public List<ProcessingResult> processBatch(List<DocumentPair> documents) {
        logger.info("Starting batch processing of {} documents", documents.size());

        List<Future<ProcessingResult>> futures = new ArrayList<>();

        for (DocumentPair pair : documents) {
            Future<ProcessingResult> future = executorService.submit(() -> {
                ProcessingResult result = processDocuments(
                    pair.getAdfContent(), pair.getAdfFilename(),
                    pair.getTaxFormContent(), pair.getTaxFormFilename()
                );
                result.addMetadata("reference_id", pair.getReferenceId());
                return result;
            });
            futures.add(future);
        }

        List<ProcessingResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                ProcessingResult result = futures.get(i).get(60, TimeUnit.SECONDS);
                results.add(result);
            } catch (Exception e) {
                logger.error("Batch processing failed for document {}", documents.get(i).getReferenceId(), e);
                ProcessingResult errorResult = new ProcessingResult();
                errorResult.setDecision("ERROR");
                errorResult.addAuditEntry("Batch processing failed: " + e.getMessage());
                errorResult.addMetadata("reference_id", documents.get(i).getReferenceId());
                results.add(errorResult);
            }
        }

        logger.info("Batch processing complete. Processed {} documents", results.size());
        return results;
    }

    /**
     * Shutdown the orchestrator's thread pool.
     */
    public void shutdown() {
        logger.info("Shutting down orchestrator");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Orchestrator shutdown complete");
    }
}