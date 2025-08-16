package com.loantrading.matching.entity;

/**
 * Enum representing the type of entity (managed fund vs standalone)
 */
public enum EntityType {
    MANAGED_FUND("Managed Fund", "Entity with a fund manager"),
    STANDALONE("Standalone", "Entity that trades directly"),
    UNKNOWN("Unknown", "Entity type could not be determined");
    
    private final String displayName;
    private final String description;
    
    EntityType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}