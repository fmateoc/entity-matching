package com.loantrading.matching.entity;

import java.util.*;

/**
 * Represents an entity extracted from external documents (ADF or Tax forms)
 */
public class ExtractedEntity {
    private String legalName;
    private String fundManager;
    private String mei;
    private String lei;
    private String ein;
    private String debtDomainId;
    private String emailDomain;
    private String dba;
    private String countryCode;
    private String taxCountryCode;
    private Map<String, String> rawFields;
    private List<String> contactEmails;
    private EntityType inferredType;
    private double extractionConfidence;
    private Map<String, Double> fieldConfidence;
    
    public ExtractedEntity() {
        this.rawFields = new HashMap<>();
        this.contactEmails = new ArrayList<>();
        this.fieldConfidence = new HashMap<>();
    }
    
    // Getters and setters
    public String getLegalName() { 
        return legalName; 
    }
    
    public void setLegalName(String legalName) { 
        this.legalName = legalName; 
    }
    
    public String getFundManager() { 
        return fundManager; 
    }
    
    public void setFundManager(String fundManager) { 
        this.fundManager = fundManager; 
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
    
    public String getEmailDomain() { 
        return emailDomain; 
    }
    
    public void setEmailDomain(String emailDomain) { 
        this.emailDomain = emailDomain; 
    }
    
    public String getDba() { 
        return dba; 
    }
    
    public void setDba(String dba) { 
        this.dba = dba; 
    }
    
    public String getCountryCode() { 
        return countryCode; 
    }
    
    public void setCountryCode(String countryCode) { 
        this.countryCode = countryCode; 
    }
    
    public String getTaxCountryCode() { 
        return taxCountryCode; 
    }
    
    public void setTaxCountryCode(String taxCountryCode) { 
        this.taxCountryCode = taxCountryCode; 
    }
    
    public Map<String, String> getRawFields() { 
        return rawFields; 
    }
    
    public void setRawFields(Map<String, String> rawFields) { 
        this.rawFields = rawFields; 
    }
    
    public List<String> getContactEmails() { 
        return contactEmails; 
    }
    
    public void setContactEmails(List<String> contactEmails) { 
        this.contactEmails = contactEmails; 
    }
    
    public EntityType getInferredType() { 
        return inferredType; 
    }
    
    public void setInferredType(EntityType inferredType) { 
        this.inferredType = inferredType; 
    }
    
    public double getExtractionConfidence() { 
        return extractionConfidence; 
    }
    
    public void setExtractionConfidence(double extractionConfidence) { 
        this.extractionConfidence = extractionConfidence; 
    }
    
    public Map<String, Double> getFieldConfidence() { 
        return fieldConfidence; 
    }
    
    public void setFieldConfidence(String field, double confidence) { 
        this.fieldConfidence.put(field, confidence); 
    }
    
    @Override
    public String toString() {
        return "ExtractedEntity{" +
                "legalName='" + legalName + '\'' +
                ", fundManager='" + fundManager + '\'' +
                ", mei='" + mei + '\'' +
                ", lei='" + lei + '\'' +
                ", ein='" + ein + '\'' +
                ", emailDomain='" + emailDomain + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", inferredType=" + inferredType +
                ", extractionConfidence=" + extractionConfidence +
                '}';
    }
}