package com.loantrading.matching.extraction;

/**
 * Exception thrown when document extraction fails
 */
public class ExtractionException extends Exception {
    
    public ExtractionException(String message) {
        super(message);
    }
    
    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ExtractionException(Throwable cause) {
        super(cause);
    }
}