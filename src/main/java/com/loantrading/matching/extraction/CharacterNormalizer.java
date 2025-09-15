package com.loantrading.matching.extraction;

import com.ibm.icu.text.Transliterator;
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
    private static final Transliterator DIACRITIC_REMOVER = Transliterator.getInstance("Any-Latin; Latin-ASCII");
    
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
     * Normalize text for Unicode, punctuation, and whitespace, without applying opinionated OCR fixes.
     * This is useful for structured text that is not from a scanned document.
     */
    public String normalizeUnicodeAndPunctuation(String text) {
        if (text == null) return "";

        String normalized = text;

        // Handle diacritics using ICU4J
        normalized = DIACRITIC_REMOVER.transliterate(normalized);

        // Apply replacements for smart quotes, dashes, and unicode spaces
        normalized = normalized.replaceAll("[“”]", "\"");
        normalized = normalized.replaceAll("[‘’]", "'");
        normalized = normalized.replaceAll("[`´]", "'");
        normalized = normalized.replaceAll("[—–]", "-");
        normalized = normalized.replaceAll("[‒―]", "-");
        normalized = normalized.replaceAll("[\\u00A0\\u2000-\\u200B\\u202F\\u205F\\u3000]", " ");

        // Remove control and zero-width characters
        normalized = normalized.replaceAll("[\\u0000-\\u001F\\u007F-\\u009F]", "");
        normalized = normalized.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");

        // Normalize standard whitespace
        normalized = normalized.replaceAll("\\s+", " ");

        return normalized.trim();
    }

    /**
     * Apply aggressive OCR error corrections.
     * This should only be used on text known to be from OCR.
     */
    public String fixOcrErrors(String text) {
        if (text == null) return "";
        // Apply OCR-specific replacements
        text = text.replaceAll("rn", "m");
        text = text.replaceAll("l([0-9])", "1$1");
        text = text.replaceAll("O([0-9])", "0$1");
        text = text.replaceAll("([0-9])O", "$10");
        text = text.replaceAll("([0-9])l", "$11");
        text = fixOCRPunctuation(text);
        return text;
    }

    /**
     * Normalize text to handle various encoding and OCR issues.
     * This is the master method that performs all normalization steps.
     */
    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        
        String normalized = normalizeUnicodeAndPunctuation(text);
        normalized = fixOcrErrors(normalized);
        
        if (logger.isDebugEnabled() && !text.equals(normalized)) {
            logger.debug("Normalized {} characters to {} characters", 
                text.length(), normalized.length());
        }
        
        return normalized.trim(); // Re-trim after all operations
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