package com.loantrading.matching.orchestrator;

/**
 * Container for a pair of documents to be processed together
 */
public class DocumentPair {
    private final byte[] adfContent;
    private final String adfFilename;
    private final byte[] taxFormContent;
    private final String taxFormFilename;
    private final String referenceId;
    
    /**
     * Create a document pair with both ADF and tax form
     */
    public DocumentPair(byte[] adfContent, String adfFilename,
                       byte[] taxFormContent, String taxFormFilename,
                       String referenceId) {
        this.adfContent = adfContent;
        this.adfFilename = adfFilename;
        this.taxFormContent = taxFormContent;
        this.taxFormFilename = taxFormFilename;
        this.referenceId = referenceId;
    }
    
    /**
     * Create a document pair with only ADF
     */
    public DocumentPair(byte[] adfContent, String adfFilename, String referenceId) {
        this(adfContent, adfFilename, null, null, referenceId);
    }
    
    public byte[] getAdfContent() {
        return adfContent;
    }
    
    public String getAdfFilename() {
        return adfFilename;
    }
    
    public byte[] getTaxFormContent() {
        return taxFormContent;
    }
    
    public String getTaxFormFilename() {
        return taxFormFilename;
    }
    
    public String getReferenceId() {
        return referenceId;
    }
    
    public boolean hasTaxForm() {
        return taxFormContent != null;
    }
    
    @Override
    public String toString() {
        return String.format("DocumentPair{ref=%s, adf=%s, taxForm=%s}",
            referenceId,
            adfFilename,
            taxFormFilename != null ? taxFormFilename : "none");
    }
}