package com.loantrading.matching.extraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes text to handle encoding issues and OCR artifacts
 */
public class CharacterNormalizer {
    private static final Logger logger = LoggerFactory.getLogger(CharacterNormalizer.class);
    
    private static final Map<String, String> REPLACEMENTS = new HashMap<>();
    
    static {
        // Smart quotes and apostrophes
        REPLACEMENTS.put("[“”]", "\"");
        REPLACEMENTS.put("[‘’]", "'");
        REPLACEMENTS.put("[`´]", "'");
        
        // Dashes
        REPLACEMENTS.put("[—–]", "-");
        REPLACEMENTS.put("[‒―]", "-");
        
        // Spaces (various Unicode spaces)
        REPLACEMENTS.put("[\\u00A0\\u2000-\\u200B\\u202F\\u205F\\u3000]", " ");
        
        // Common OCR confusions
        REPLACEMENTS.put("rn", "m");  // Common OCR mistake
        REPLACEMENTS.put("l([0-9])", "1$1");  // l confused with 1
        REPLACEMENTS.put("O([0-9])", "0$1");  // O confused with 0
        REPLACEMENTS.put("([0-9])O", "$10");  // O confused with 0
        REPLACEMENTS.put("([0-9])l", "$11");  // l confused with 1
    }
    
    /**
     * Normalize text to handle various encoding and OCR issues
     */
    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        
        String normalized = text;

        // Handle diacritics
        normalized = java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        
        // Apply replacements
        for (Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
            normalized = normalized.replaceAll(entry.getKey(), entry.getValue());
        }
        
        // Remove control characters
        normalized = normalized.replaceAll("[\\u0000-\\u001F\\u007F-\\u009F]", "");
        
        // Remove zero-width characters
        normalized = normalized.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");
        
        // Normalize whitespace
        normalized = normalized.replaceAll("\\s+", " ");
        
        // Fix common OCR issues with punctuation
        normalized = fixOCRPunctuation(normalized);
        
        // Trim
        normalized = normalized.trim();
        
        if (logger.isDebugEnabled() && !text.equals(normalized)) {
            logger.debug("Normalized {} characters to {} characters", 
                text.length(), normalized.length());
        }
        
        return normalized;
    }
    
    /**
     * Fix common OCR punctuation errors
     */
    private String fixOCRPunctuation(String text) {
        // Fix periods that should be commas in numbers
        text = text.replaceAll("(\\d)\\.{2,}(\\d)", "$1,$2");
        
        // Fix multiple consecutive punctuation
        text = text.replaceAll("\\.{2,}", ".");
        text = text.replaceAll(",{2,}", ",");
        text = text.replaceAll("-{2,}", "-");
        
        // Fix spaces around punctuation
        text = text.replaceAll("\\s+([.,;:!?])", "$1");
        text = text.replaceAll("([.,;:!?])([A-Za-z])", "$1 $2");
        
        return text;
    }
    
    /**
     * Clean text specifically for entity names
     */
    public String cleanEntityName(String name) {
        if (name == null) {
            return null;
        }
        
        // First normalize
        name = normalize(name);
        
        // Remove brackets and their contents (often contain codes or notes)
        name = name.replaceAll("\\[[^\\]]*\\]", "");
        name = name.replaceAll("\\([^\\)]*\\)", "");
        
        // Remove quotes
        name = name.replaceAll("[\"\']", "");
        
        // Fix spacing
        name = name.replaceAll("\\s+", " ");
        
        return name.trim();
    }
}