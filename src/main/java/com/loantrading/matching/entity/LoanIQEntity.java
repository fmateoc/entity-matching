package com.loantrading.matching.entity;

import java.time.LocalDateTime;

/**
 * Represents an entity from the LoanIQ database
 */
public class LoanIQEntity {
    private Long entityId;
    private String fullName;
    private String shortName;
    private String ultimateParent; // Repurposed for fund manager
    private String mei;
    private String lei;
    private String ein;
    private String debtDomainId;
    private String countryCode;
    private String legalAddress;
    private String taxAddress;
    private boolean isLocation;
    private Long parentCustomerId;
    private LocalDateTime lastModified;
    
    // Getters and setters
    public Long getEntityId() { 
        return entityId; 
    }
    
    public void setEntityId(Long entityId) { 
        this.entityId = entityId; 
    }
    
    public String getFullName() { 
        return fullName; 
    }
    
    public void setFullName(String fullName) { 
        this.fullName = fullName; 
    }
    
    public String getShortName() { 
        return shortName; 
    }
    
    public void setShortName(String shortName) { 
        this.shortName = shortName; 
    }
    
    public String getUltimateParent() { 
        return ultimateParent; 
    }
    
    public void setUltimateParent(String ultimateParent) { 
        this.ultimateParent = ultimateParent; 
    }
    
    public String getMei() { 
        return mei; 
    }
    
    public void setMei(String mei) { 
        this.mei = mei; 
    }
    
    public String getLei() { 
        return lei; 
    }
    
    public void setLei(String lei) { 
        this.lei = lei; 
    }
    
    public String getEin() { 
        return ein; 
    }
    
    public void setEin(String ein) { 
        this.ein = ein; 
    }
    
    public String getDebtDomainId() { 
        return debtDomainId; 
    }
    
    public void setDebtDomainId(String debtDomainId) { 
        this.debtDomainId = debtDomainId; 
    }
    
    public String getCountryCode() { 
        return countryCode; 
    }
    
    public void setCountryCode(String countryCode) { 
        this.countryCode = countryCode; 
    }
    
    public String getLegalAddress() { 
        return legalAddress; 
    }
    
    public void setLegalAddress(String legalAddress) { 
        this.legalAddress = legalAddress; 
    }
    
    public String getTaxAddress() { 
        return taxAddress; 
    }
    
    public void setTaxAddress(String taxAddress) { 
        this.taxAddress = taxAddress; 
    }
    
    public boolean isLocation() { 
        return isLocation; 
    }
    
    public void setLocation(boolean location) { 
        isLocation = location; 
    }
    
    public Long getParentCustomerId() { 
        return parentCustomerId; 
    }
    
    public void setParentCustomerId(Long parentCustomerId) { 
        this.parentCustomerId = parentCustomerId; 
    }
    
    public LocalDateTime getLastModified() { 
        return lastModified; 
    }
    
    public void setLastModified(LocalDateTime lastModified) { 
        this.lastModified = lastModified; 
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanIQEntity that = (LoanIQEntity) o;
        return entityId != null && entityId.equals(that.entityId);
    }
    
    @Override
    public int hashCode() {
        return entityId != null ? entityId.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "LoanIQEntity{" +
                "entityId=" + entityId +
                ", fullName='" + fullName + '\'' +
                ", shortName='" + shortName + '\'' +
                ", mei='" + mei + '\'' +
                ", isLocation=" + isLocation +
                '}';
    }
}