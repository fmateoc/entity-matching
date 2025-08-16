package com.loantrading.matching.extraction;

/**
 * Result of OCR processing including text and confidence score
 */
public class OCRResult {
    private final String text;
    private final double confidence;
    
    public OCRResult(String text, double confidence) {
        this.text = text;
        this.confidence = confidence;
    }
    
    public String getText() {
        return text;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public boolean isHighConfidence() {
        return confidence > 0.8;
    }
    
    public boolean isLowConfidence() {
        return confidence < 0.5;
    }
    
    @Override
    public String toString() {
        return String.format("OCRResult{confidence=%.2f, textLength=%d}", 
            confidence, text != null ? text.length() : 0);
    }
}