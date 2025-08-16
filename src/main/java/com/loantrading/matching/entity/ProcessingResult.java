package com.loantrading.matching.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the complete result of processing documents through the matching system
 */
public class ProcessingResult {
    private ExtractedEntity extractedData;
    private ExtractedEntity taxFormData;
    private EntityType entityType;
    private List<MatchResult> topMatches;
    private MatchResult selectedMatch;
    private String decision; // MATCH, NO_MATCH, MANUAL_REVIEW, ERROR
    private List<String> auditTrail;
    private LocalDateTime processedAt;
    private long processingTimeMs;
    private Map<String, Object> metadata;
    
    public ProcessingResult() {
        this.topMatches = new ArrayList<>();
        this.auditTrail = new ArrayList<>();
        this.processedAt = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }
    
    // Getters and setters
    public ExtractedEntity getExtractedData() {
        return extractedData;
    }
    
    public void setExtractedData(ExtractedEntity extractedData) {
        this.extractedData = extractedData;
    }
    
    public ExtractedEntity getTaxFormData() {
        return taxFormData;
    }
    
    public void setTaxFormData(ExtractedEntity taxFormData) {
        this.taxFormData = taxFormData;
    }
    
    public EntityType getEntityType() {
        return entityType;
    }
    
    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }
    
    public List<MatchResult> getTopMatches() {
        return topMatches;
    }
    
    public void addMatch(MatchResult match) {
        this.topMatches.add(match);
    }
    
    public MatchResult getSelectedMatch() {
        return selectedMatch;
    }
    
    public void setSelectedMatch(MatchResult selectedMatch) {
        this.selectedMatch = selectedMatch;
    }
    
    public String getDecision() {
        return decision;
    }
    
    public void setDecision(String decision) {
        this.decision = decision;
    }
    
    public List<String> getAuditTrail() {
        return auditTrail;
    }
    
    public void addAuditEntry(String entry) {
        this.auditTrail.add(String.format("[%s] %s",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATETIME), entry));
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public boolean isSuccessful() {
        return !"ERROR".equals(decision);
    }
    
    public boolean requiresReview() {
        return "MANUAL_REVIEW".equals(decision);
    }
    
    @Override
    public String toString() {
        return String.format("ProcessingResult{decision=%s, entityType=%s, matches=%d, processingTime=%dms}",
            decision,
            entityType,
            topMatches.size(),
            processingTimeMs);
    }
}