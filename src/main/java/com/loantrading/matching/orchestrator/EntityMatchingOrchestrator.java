package com.loantrading.matching.orchestrator;

import com.loantrading.matching.detection.EntityTypeDetector;
import com.loantrading.matching.engine.MatchingEngine;
import com.loantrading.matching.entity.*;
import com.loantrading.matching.extraction.MultiFormatDocumentExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;

/**
 * Main orchestrator for the entity matching process
 */
public class EntityMatchingOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(EntityMatchingOrchestrator.class);
    
    private final MultiFormatDocumentExtractor extractor;
    private final EntityTypeDetector typeDetector;
    private final MatchingEngine matchingEngine;
    private final ExecutorService executorService;
    
    public EntityMatchingOrchestrator(Connection dbConnection) {
        this.extractor = new MultiFormatDocumentExtractor();
        this.typeDetector = new EntityTypeDetector();
        this.matchingEngine = new MatchingEngine(dbConnection);
        this.executorService = Executors.newFixedThreadPool(4);
    }
    
    /**
     * Process documents and return matching results
     */
    public ProcessingResult processDocuments(byte[] adfContent, String adfFilename,
                                            byte[] taxFormContent, String taxFormFilename) {
        ProcessingResult result = new ProcessingResult();
        long startTime = System.currentTimeMillis();
        
        try {
            // Stage 1: Extract from ADF
            result.addAuditEntry("Starting ADF extraction from: " + adfFilename);
            ExtractedEntity adfData = extractor.extract(adfContent, adfFilename);
            result.setExtractedData(adfData);
            result.addAuditEntry(String.format("ADF extraction complete. Confidence: %.2f",
                adfData.getExtractionConfidence()));
            
            // Log extracted identifiers
            logExtractedIdentifiers(adfData, result);
            
            // Stage 2: Extract from tax form if available (parallel)
            Future<ExtractedEntity> taxFormFuture = null;
            if (taxFormContent != null) {
                result.addAuditEntry("Starting tax form extraction from: " + taxFormFilename);
                taxFormFuture = executorService.submit(() -> {
                    try {
                        return extractor.extract(taxFormContent, taxFormFilename);
                    } catch (Exception e) {
                        logger.error("Tax form extraction failed", e);
                        return null;
                    }
                });
            }
            
            // Stage 3: Determine entity type
            EntityType entityType = typeDetector.detectType(adfData);
            result.setEntityType(entityType);
            result.addAuditEntry("Entity type detected: " + entityType);
            result.addMetadata("entity_type", entityType);
            
            // Get tax form result if processing
            ExtractedEntity taxFormData = null;
            if (taxFormFuture != null) {
                try {
                    taxFormData = taxFormFuture.get(30, TimeUnit.SECONDS);
                    if (taxFormData != null) {
                        result.setTaxFormData(taxFormData);
                        result.addAuditEntry(String.format("Tax form extraction complete. Confidence: %.2f",
                            taxFormData.getExtractionConfidence()));
                        logExtractedIdentifiers(taxFormData, result);
                    }
                } catch (TimeoutException e) {
                    result.addAuditEntry("Tax form extraction timed out");
                }
            }
            
            // Stage 4: Find matches
            result.addAuditEntry("Starting entity matching");
            List<MatchResult> matches = matchingEngine.findMatches(adfData, taxFormData);
            
            // Add top 5 matches to result
            for (int i = 0; i < Math.min(5, matches.size()); i++) {
                MatchResult match = matches.get(i);
                result.addMatch(match);
                result.addAuditEntry(String.format("Match %d: %s (Score: %.2f, Strategy: %s)",
                    i + 1,
                    match.getMatchedEntity().getFullName(),
                    match.getScore(),
                    match.getMatchStrategy()));
            }
            
            // Stage 5: Select best match and make decision
            if (!matches.isEmpty()) {
                MatchResult bestMatch = matches.get(0);
                result.setSelectedMatch(bestMatch);
                
                // Determine decision based on confidence
                String decision = determineDecision(bestMatch);
                result.setDecision(decision);
                
                result.addAuditEntry(String.format(
                    "Best match selected: %s (ID: %d, Score: %.2f, Confidence: %s, Decision: %s)",
                    bestMatch.getMatchedEntity().getFullName(),
                    bestMatch.getMatchedEntity().getEntityId(),
                    bestMatch.getScore(),
                    bestMatch.getConfidence(),
                    decision
                ));
                
                // Log discrepancies
                logDiscrepancies(bestMatch, result);
                
                // Log potential duplicates
                if (!bestMatch.getPotentialDuplicates().isEmpty()) {
                    result.addAuditEntry(String.format("Potential duplicates detected: %d",
                        bestMatch.getPotentialDuplicates().size()));
                    result.addMetadata("duplicate_count", bestMatch.getPotentialDuplicates().size());
                }
            } else {
                result.setDecision("NO_MATCH");
                result.addAuditEntry("No matches found - new entity");
            }
            
            // Record processing time
            long processingTime = System.currentTimeMillis() - startTime;
            result.setProcessingTimeMs(processingTime);
            result.addAuditEntry(String.format("Processing completed in %d ms", processingTime));
            result.addMetadata("processing_time_ms", processingTime);
            
        } catch (Exception e) {
            logger.error("Processing failed", e);
            result.addAuditEntry("Processing failed: " + e.getMessage());
            result.setDecision("ERROR");
            result.addMetadata("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Process a batch of documents
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
                logger.error("Batch processing failed for document {}", i, e);
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
     * Determine decision based on match confidence
     */
    private String determineDecision(MatchResult match) {
        // Decision thresholds
        if (match.getScore() >= 85) {
            return "MATCH";
        } else if (match.getScore() >= 70) {
            // Check for critical discrepancies
            boolean hasCritical = match.hasCriticalDiscrepancies();
            
            if (hasCritical) {
                return "MANUAL_REVIEW";
            }
            return "MATCH";
        } else if (match.getScore() >= 50) {
            return "MANUAL_REVIEW";
        } else {
            return "NO_MATCH";
        }
    }
    
    /**
     * Log extracted identifiers to audit trail
     */
    private void logExtractedIdentifiers(ExtractedEntity entity, ProcessingResult result) {
        if (entity.getMei() != null) {
            result.addAuditEntry("  MEI: " + entity.getMei());
        }
        if (entity.getLei() != null) {
            result.addAuditEntry("  LEI: " + entity.getLei());
        }
        if (entity.getEin() != null) {
            result.addAuditEntry("  EIN: " + entity.getEin());
        }
        if (entity.getDebtDomainId() != null) {
            result.addAuditEntry("  Debt Domain ID: " + entity.getDebtDomainId());
        }
        if (entity.getEmailDomain() != null) {
            result.addAuditEntry("  Email Domain: " + entity.getEmailDomain());
        }
    }
    
    /**
     * Log discrepancies to audit trail
     */
    private void logDiscrepancies(MatchResult match, ProcessingResult result) {
        if (!match.getDiscrepancies().isEmpty()) {
            result.addAuditEntry(String.format("Found %d discrepancies:",
                match.getDiscrepancies().size()));
            
            // Group by severity
            Map<DiscrepancySeverity, Integer> severityCounts = new HashMap<>();
            for (Discrepancy disc : match.getDiscrepancies()) {
                severityCounts.merge(disc.getSeverity(), 1, Integer::sum);
                
                // Log critical and high severity discrepancies
                if (disc.getSeverity() == DiscrepancySeverity.CRITICAL ||
                    disc.getSeverity() == DiscrepancySeverity.HIGH) {
                    result.addAuditEntry(String.format("  - [%s] %s: %s",
                        disc.getSeverity(), disc.getType(), disc.getDescription()));
                }
            }
            
            result.addMetadata("discrepancy_counts", severityCounts);
        }
    }
    
    /**
     * Shutdown the orchestrator
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
        
        matchingEngine.close();
        logger.info("Orchestrator shutdown complete");
    }
}