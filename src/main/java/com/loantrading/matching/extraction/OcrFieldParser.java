package com.loantrading.matching.extraction;

import com.loantrading.matching.entity.ExtractedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A dedicated parser for handling messy, unlabeled text from OCR.
 * It uses a stateful, subtractive approach to identify fields.
 */
public class OcrFieldParser {
    private static final Logger logger = LoggerFactory.getLogger(OcrFieldParser.class);

    private static final Pattern MEI_PATTERN = Pattern.compile("([A-Z]{2}\\d{8})\\b");
    private static final Pattern LEI_PATTERN = Pattern.compile("([A-Z0-9]{20})\\b");
    private static final Pattern EIN_PATTERN = Pattern.compile("(\\d{2}-?\\d{7})\\b");

    // A list of common corporate suffixes to help identify company names in messy text.
    private static final List<String> CORPORATE_SUFFIXES = Arrays.asList(
            "lp", "llp", "llc", "inc", "ltd", "corp", "co", "gmbh", "sarl", "bv", "ag"
    );

    private final CharacterNormalizer normalizer;

    public OcrFieldParser() {
        this.normalizer = new CharacterNormalizer();
    }

    public ExtractedEntity parse(String text) {
        ExtractedEntity entity = new ExtractedEntity();
        entity.setRawFields(new HashMap<>());

        String remainingText = normalizer.normalizeUnicodeAndPunctuation(text);
        logger.debug("Starting OCR parsing for text:\n---\n{}\n---", remainingText);

        remainingText = extractIdentifiers(remainingText, entity);
        extractLegalNameAndManager(remainingText, entity);

        logger.info("OCR parsing complete. Name: [{}], Manager: [{}], MEI: [{}], LEI: [{}], EIN: [{}]",
                entity.getLegalName(), entity.getFundManager(), entity.getMei(), entity.getLei(), entity.getEin());

        return entity;
    }

    private String extractIdentifiers(String text, ExtractedEntity entity) {
        String currentText = text;

        Matcher meiMatcher = MEI_PATTERN.matcher(currentText);
        if (meiMatcher.find()) {
            entity.setMei(meiMatcher.group(1));
            logger.debug("Extracted MEI from OCR: {}", entity.getMei());
            currentText = meiMatcher.replaceFirst("");
        }

        Matcher leiMatcher = LEI_PATTERN.matcher(currentText);
        if (leiMatcher.find()) {
            entity.setLei(leiMatcher.group(1));
            logger.debug("Extracted LEI from OCR: {}", entity.getLei());
            currentText = leiMatcher.replaceFirst("");
        }

        Matcher einMatcher = EIN_PATTERN.matcher(currentText);
        if (einMatcher.find()) {
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
        String cleanedText = cleanupAllLabels(text);
        List<String> lines = Arrays.stream(cleanedText.trim().split("\n"))
                                   .map(String::trim)
                                   .filter(l -> !l.isEmpty() && l.length() > 3)
                                   .collect(Collectors.toList());

        if (lines.isEmpty()) {
            return;
        }

        // Heuristic: The legal name is often the line that contains a corporate suffix.
        String legalName = lines.stream()
                .filter(this::hasCorporateSuffix)
                .max(Comparator.comparingInt(String::length))
                .orElse(null);

        // If no line has a suffix, fall back to the longest line.
        if (legalName == null) {
            legalName = lines.stream()
                    .max(Comparator.comparingInt(String::length))
                    .orElse("");
        }

        if (!legalName.isEmpty()) {
            entity.setLegalName(normalizer.cleanEntityName(legalName));
            logger.debug("Extracted Legal Name from OCR: {}", entity.getLegalName());
        }

        // The fund manager is often the second-longest line, if it exists.
        if (lines.size() > 1) {
            final String finalLegalName = legalName;
            String fundManager = lines.stream()
                    .filter(l -> !l.equalsIgnoreCase(finalLegalName))
                    .max(Comparator.comparingInt(String::length))
                    .orElse("");

            if (!fundManager.isEmpty()) {
                entity.setFundManager(normalizer.cleanEntityName(fundManager));
                logger.debug("Extracted Fund Manager from OCR: {}", entity.getFundManager());
            }
        }
    }

    private boolean hasCorporateSuffix(String line) {
        String lowerCaseLine = line.toLowerCase();
        return CORPORATE_SUFFIXES.stream().anyMatch(suffix -> lowerCaseLine.endsWith("." + suffix) || lowerCaseLine.endsWith(" " + suffix));
    }

    private String cleanupAllLabels(String text) {
        return text.replaceAll("(?i)(Legal Name|Entity Name|Fund Manager|Address|MEI|LEI|EIN)[:\\s]+", "").trim();
    }
}