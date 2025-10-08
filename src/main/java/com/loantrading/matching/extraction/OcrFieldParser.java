package com.loantrading.matching.extraction;

import com.loantrading.matching.entity.ExtractedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A dedicated parser for handling messy, unlabeled text from OCR.
 * It uses a stateful, subtractive approach to identify fields.
 */
public class OcrFieldParser {
    private static final Logger logger = LoggerFactory.getLogger(OcrFieldParser.class);

    // More flexible patterns for OCR, looking for identifiers without strict labels.
    private static final Pattern MEI_PATTERN = Pattern.compile("([A-Z]{2}\\d{8})\\b");
    private static final Pattern LEI_PATTERN = Pattern.compile("([A-Z0-9]{20})\\b");
    private static final Pattern EIN_PATTERN = Pattern.compile("(\\d{2}-?\\d{7})\\b");

    private final CharacterNormalizer normalizer;

    public OcrFieldParser() {
        this.normalizer = new CharacterNormalizer();
    }

    public ExtractedEntity parse(String text) {
        ExtractedEntity entity = new ExtractedEntity();
        entity.setRawFields(new HashMap<>());

        // First, normalize the entire block of text.
        String remainingText = normalizer.normalizeUnicodeAndPunctuation(text);
        logger.debug("Starting OCR parsing for text:\n---\n{}\n---", remainingText);

        // Extract all known identifiers, removing them from the text block.
        remainingText = extractIdentifiers(remainingText, entity);

        // From the remaining text, extract the names.
        extractLegalNameAndManager(remainingText, entity);

        logger.info("OCR parsing complete. Name: [{}], Manager: [{}], MEI: [{}], LEI: [{}], EIN: [{}]",
                entity.getLegalName(), entity.getFundManager(), entity.getMei(), entity.getLei(), entity.getEin());

        return entity;
    }

    private String extractIdentifiers(String text, ExtractedEntity entity) {
        String currentText = text;

        // Find and remove MEI
        Matcher meiMatcher = MEI_PATTERN.matcher(currentText);
        if (meiMatcher.find()) {
            entity.setMei(meiMatcher.group(1));
            logger.debug("Extracted MEI from OCR: {}", entity.getMei());
            currentText = meiMatcher.replaceFirst("");
        }

        // Find and remove LEI
        Matcher leiMatcher = LEI_PATTERN.matcher(currentText);
        if (leiMatcher.find()) {
            entity.setLei(leiMatcher.group(1));
            logger.debug("Extracted LEI from OCR: {}", entity.getLei());
            currentText = leiMatcher.replaceFirst("");
        }

        // Find and remove EIN
        Matcher einMatcher = EIN_PATTERN.matcher(currentText);
        if (einMatcher.find()) {
            // Normalize EIN to XX-XXXXXXX format
            String rawEin = einMatcher.group(1).replaceAll("-", "");
            if (rawEin.length() == 9) {
                String formattedEin = rawEin.substring(0, 2) + "-" + rawEin.substring(2);
                entity.setEin(formattedEin);
                logger.debug("Extracted EIN from OCR: {}", entity.getEin());
                currentText = einMatcher.replaceFirst("");
            }
        }

        return currentText;
    }

    private void extractLegalNameAndManager(String text, ExtractedEntity entity) {
        // After identifiers are removed, the remaining text is primarily names and labels.
        // Clean up all known labels first.
        String cleanedText = cleanupAllLabels(text);

        String[] lines = cleanedText.trim().split("\n");

        // Find the longest remaining line for the legal name.
        String legalName = Arrays.stream(lines)
                .map(String::trim)
                .filter(l -> !l.isEmpty() && l.length() > 3) // Filter out very short lines
                .max(Comparator.comparingInt(String::length))
                .orElse("");

        if (!legalName.isEmpty()) {
            entity.setLegalName(normalizer.cleanEntityName(legalName));
            logger.debug("Extracted Legal Name from OCR: {}", entity.getLegalName());
        }

        // Find the second-longest line for the fund manager.
        if (lines.length > 1) {
            final String finalLegalName = legalName; // Final variable for use in lambda
            String fundManager = Arrays.stream(lines)
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.equalsIgnoreCase(finalLegalName))
                    .max(Comparator.comparingInt(String::length))
                    .orElse("");

            if (!fundManager.isEmpty()) {
                entity.setFundManager(normalizer.cleanEntityName(fundManager));
                logger.debug("Extracted Fund Manager from OCR: {}", entity.getFundManager());
            }
        }
    }

    private String cleanupAllLabels(String text) {
        // Remove all common labels, regardless of position.
        return text.replaceAll("(?i)(Legal Name|Entity Name|Fund Manager|Address|MEI|LEI|EIN)[:\\s]+", "").trim();
    }
}