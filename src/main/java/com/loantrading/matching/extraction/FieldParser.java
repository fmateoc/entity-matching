package com.loantrading.matching.extraction;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.util.CountryCodeValidator;
import com.loantrading.matching.util.ServiceProviderDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses structured fields from extracted text
 */
public class FieldParser {
    private static final Logger logger = LoggerFactory.getLogger(FieldParser.class);
    
    // Identifier patterns
    private static final Pattern MEI_PATTERN = Pattern.compile(
        "(?:MEI|Member\\s*Entity\\s*ID|Member\\s*ID)[:\\s]*([A-Z]{2}\\d{8})\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LEI_PATTERN = Pattern.compile(
        "(?:LEI|Legal\\s*Entity\\s*ID)[:\\s]*([A-Z0-9]{20})\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EIN_PATTERN = Pattern.compile(
        "(?:EIN|TIN|Tax\\s*ID|Federal\\s*Tax\\s*ID)[:\\s]*(\\d{2}-?\\d{7})\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DEBT_DOMAIN_PATTERN = Pattern.compile(
        "(?:Debt\\s*Domain\\s*ID|DD\\s*ID)[:\\s]*([A-Z0-9]{6,12})\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "([a-zA-Z0-9][a-zA-Z0-9._%+-]*@[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,})"
    );
    
    // Name patterns
    private static final List<Pattern> LEGAL_NAME_PATTERNS = Arrays.asList(
        Pattern.compile("(?:Legal\\s+Name|Entity\\s+Name|Lender\\s+Name|Name\\s+of\\s+Lender)[:\\s]+([^\\n]+)", 
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:Participant|Borrower|Obligor)\\s+Name[:\\s]+([^\\n]+)", 
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("^([A-Z][A-Za-z\\s&,.-]+(?:Inc|LLC|Ltd|LP|LLP|Corp|Company|PLC|SA|GmbH|BV|NV))\\.?", 
            Pattern.MULTILINE)
    );
    
    private static final List<Pattern> FUND_MANAGER_PATTERNS = Arrays.asList(
        Pattern.compile("(?:Fund\\s+Manager|Investment\\s+Manager|Advisor|Asset\\s+Manager)[:\\s]+([^\\n]+)", 
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:Managed\\s+by|Advised\\s+by)[:\\s]+([^\\n]+)", 
            Pattern.CASE_INSENSITIVE)
    );
    
    private static final Pattern DBA_PATTERN = Pattern.compile(
        "(?:DBA|d/b/a|Doing\\s+Business\\s+As|Trade\\s+Name|Trading\\s+As)[:\\s]+([^\\n]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Country patterns
    private static final List<Pattern> COUNTRY_PATTERNS = Arrays.asList(
        Pattern.compile("(?:Country|Jurisdiction|Incorporated\\s+in)[:\\s]+([A-Z]{2,3}|[A-Za-z\\s]+)", 
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:Address|Located\\s+in)[:\\s]+[^,]+,\\s*([A-Z]{2,3}|[A-Za-z\\s]+)$", 
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE)
    );
    
    private final CharacterNormalizer normalizer;
    private final CountryCodeValidator countryValidator;
    private final ServiceProviderDetector serviceProviderDetector;
    
    public FieldParser() {
        this.normalizer = new CharacterNormalizer();
        this.countryValidator = new CountryCodeValidator();
        this.serviceProviderDetector = new ServiceProviderDetector();
    }
    
    /**
     * Parse all fields from extracted text
     */
    public ExtractedEntity parseFields(String text) {
        ExtractedEntity entity = new ExtractedEntity();
        entity.setRawFields(new HashMap<>());
        
        // Clean text first
        text = cleanText(text);
        
        // Extract identifiers with confidence scoring
        extractMEI(text, entity);
        extractLEI(text, entity);
        extractEIN(text, entity);
        extractDebtDomainId(text, entity);
        
        // Extract names
        extractLegalName(text, entity);
        extractFundManager(text, entity);
        extractDBA(text, entity);
        
        // Extract contact information
        extractEmails(text, entity);
        
        // Extract geographic info
        extractCountries(text, entity);
        
        // Calculate overall extraction confidence
        calculateExtractionConfidence(entity);
        
        logger.info("Parsed entity with confidence: {}", entity.getExtractionConfidence());
        
        return entity;
    }
    
    private String cleanText(String text) {
        // Basic cleaning
        text = normalizer.normalize(text);
        
        // Fix common OCR errors specific to identifiers
        text = text.replaceAll("\\bMEl\\b", "MEI");
        text = text.replaceAll("\\bLEl\\b", "LEI");
        text = text.replaceAll("\\bElN\\b", "EIN");
        
        return text;
    }
    
    private void extractMEI(String text, ExtractedEntity entity) {
        Matcher matcher = MEI_PATTERN.matcher(text);
        if (matcher.find()) {
            String mei = matcher.group(1).toUpperCase();
            String countryCode = mei.substring(0, 2);
            
            if (countryValidator.isValidCountryCode(countryCode)) {
                entity.setMei(mei);
                entity.getRawFields().put("mei_raw", mei);
                entity.setFieldConfidence("mei", 0.95);
                logger.debug("MEI extracted: {}", mei);
            } else {
                logger.warn("Invalid country code in MEI: {}", mei);
                entity.setFieldConfidence("mei", 0.5);
            }
        }
    }
    
    private void extractLEI(String text, ExtractedEntity entity) {
        Matcher matcher = LEI_PATTERN.matcher(text);
        if (matcher.find()) {
            String lei = matcher.group(1).toUpperCase();
            if (isValidLEI(lei)) {
                entity.setLei(lei);
                entity.getRawFields().put("lei_raw", lei);
                entity.setFieldConfidence("lei", 0.9);
                logger.debug("LEI extracted: {}", lei);
            }
        }
    }
    
    private void extractEIN(String text, ExtractedEntity entity) {
        Matcher matcher = EIN_PATTERN.matcher(text);
        if (matcher.find()) {
            String ein = matcher.group(1);
            // Normalize format
            if (!ein.contains("-")) {
                ein = ein.substring(0, 2) + "-" + ein.substring(2);
            }
            entity.setEin(ein);
            entity.getRawFields().put("ein_raw", ein);
            entity.setFieldConfidence("ein", 0.85);
            logger.debug("EIN extracted: {}", ein);
        }
    }
    
    private void extractDebtDomainId(String text, ExtractedEntity entity) {
        Matcher matcher = DEBT_DOMAIN_PATTERN.matcher(text);
        if (matcher.find()) {
            entity.setDebtDomainId(matcher.group(1).toUpperCase());
            entity.getRawFields().put("debt_domain_id_raw", matcher.group(1));
            entity.setFieldConfidence("debt_domain_id", 0.8);
            logger.debug("Debt Domain ID extracted: {}", entity.getDebtDomainId());
        }
    }
    
    private void extractEmails(String text, ExtractedEntity entity) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        List<String> emails = new ArrayList<>();
        Map<String, Integer> domainCounts = new HashMap<>();
        
        while (matcher.find()) {
            String email = matcher.group(1).toLowerCase();
            emails.add(email);
            
            String domain = email.substring(email.indexOf('@') + 1);
            
            // Filter out service provider domains
            if (!serviceProviderDetector.isServiceProviderDomain(domain)) {
                domainCounts.merge(domain, 1, Integer::sum);
            }
        }
        
        entity.setContactEmails(emails);
        
        // Select most common non-service provider domain
        if (!domainCounts.isEmpty()) {
            String primaryDomain = domainCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            
            entity.setEmailDomain(primaryDomain);
            entity.setFieldConfidence("email_domain", 0.75);
            logger.debug("Primary email domain: {}", primaryDomain);
        }
    }
    
    private void extractLegalName(String text, ExtractedEntity entity) {
        for (Pattern pattern : LEGAL_NAME_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String legalName = normalizer.cleanEntityName(matcher.group(1));
                entity.setLegalName(legalName);
                entity.getRawFields().put("legal_name_raw", matcher.group(1));
                entity.setFieldConfidence("legal_name", 0.8);
                logger.debug("Legal name extracted: {}", legalName);
                break;
            }
        }
    }
    
    private void extractFundManager(String text, ExtractedEntity entity) {
        for (Pattern pattern : FUND_MANAGER_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String manager = normalizer.cleanEntityName(matcher.group(1));
                entity.setFundManager(manager);
                entity.getRawFields().put("fund_manager_raw", matcher.group(1));
                entity.setFieldConfidence("fund_manager", 0.75);
                logger.debug("Fund manager extracted: {}", manager);
                break;
            }
        }
    }
    
    private void extractDBA(String text, ExtractedEntity entity) {
        Matcher matcher = DBA_PATTERN.matcher(text);
        if (matcher.find()) {
            String dba = normalizer.cleanEntityName(matcher.group(1));
            entity.setDba(dba);
            entity.setFieldConfidence("dba", 0.7);
            logger.debug("DBA extracted: {}", dba);
        }
    }
    
    private void extractCountries(String text, ExtractedEntity entity) {
        // Extract from MEI if available
        if (entity.getMei() != null && entity.getMei().length() >= 2) {
            entity.setCountryCode(entity.getMei().substring(0, 2));
        }
        
        // Look for country patterns
        for (Pattern pattern : COUNTRY_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String country = countryValidator.normalizeCountry(matcher.group(1));
                if (entity.getCountryCode() == null) {
                    entity.setCountryCode(country);
                    entity.setFieldConfidence("country", 0.7);
                } else if (entity.getTaxCountryCode() == null) {
                    entity.setTaxCountryCode(country);
                }
                break;
            }
        }
    }
    
    private boolean isValidLEI(String lei) {
        if (lei == null || lei.length() != 20) return false;
        
        // Basic LEI format validation
        if (!lei.matches("[A-Z0-9]{20}")) return false;
        
        // In production, would implement full ISO 17442 checksum validation
        return true;
    }
    
    private void calculateExtractionConfidence(ExtractedEntity entity) {
        double confidence = 0.5; // Base confidence
        int fieldCount = 0;
        double fieldConfidenceSum = 0;
        
        // Weight each field by importance and presence
        if (entity.getMei() != null) {
            confidence += 0.15;
            fieldCount++;
            fieldConfidenceSum += entity.getFieldConfidence().getOrDefault("mei", 0.0);
        }
        if (entity.getLei() != null) {
            confidence += 0.10;
            fieldCount++;
            fieldConfidenceSum += entity.getFieldConfidence().getOrDefault("lei", 0.0);
        }
        if (entity.getEin() != null) {
            confidence += 0.10;
            fieldCount++;
            fieldConfidenceSum += entity.getFieldConfidence().getOrDefault("ein", 0.0);
        }
        if (entity.getLegalName() != null) {
            confidence += 0.08;
            fieldCount++;
            fieldConfidenceSum += entity.getFieldConfidence().getOrDefault("legal_name", 0.0);
        }
        if (entity.getEmailDomain() != null) {
            confidence += 0.05;
            fieldCount++;
            fieldConfidenceSum += entity.getFieldConfidence().getOrDefault("email_domain", 0.0);
        }
        if (entity.getCountryCode() != null) {
            confidence += 0.02;
            fieldCount++;
            fieldConfidenceSum += entity.getFieldConfidence().getOrDefault("country", 0.0);
        }
        
        // Adjust by average field confidence
        if (fieldCount > 0) {
            double avgFieldConfidence = fieldConfidenceSum / fieldCount;
            confidence *= avgFieldConfidence;
        }
        
        entity.setExtractionConfidence(Math.min(confidence, 1.0));
    }
}