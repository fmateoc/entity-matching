package com.loantrading.matching.entity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a discrepancy found during entity matching
 */
public class Discrepancy {
    private String type;
    private DiscrepancySeverity severity;
    private String description;
    private Map<String, Object> details;
    private String source;
    private LocalDateTime detectedAt;
    
    public Discrepancy(String type, DiscrepancySeverity severity, String description) {
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.details = new HashMap<>();
        this.detectedAt = LocalDateTime.now();
    }
    
    public void addDetail(String key, Object value) {
        details.put(key, value);
    }
    
    // Getters and setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public DiscrepancySeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(DiscrepancySeverity severity) {
        this.severity = severity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }
    
    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", severity, type, description);
    }
}