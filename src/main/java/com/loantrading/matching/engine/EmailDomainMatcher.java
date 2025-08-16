package com.loantrading.matching.engine;

import com.loantrading.matching.entity.LoanIQEntity;
import com.loantrading.matching.entity.MatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Enhances matching scores based on email domain evidence
 */
public class EmailDomainMatcher {
    private static final Logger logger = LoggerFactory.getLogger(EmailDomainMatcher.class);
    
    private static final Map<String, Set<String>> CORPORATE_DOMAINS = new HashMap<>();
    
    static {
        // Map domains to corporate entity names
        CORPORATE_DOMAINS.put("blackrock.com", new HashSet<>(Arrays.asList(
            "blackrock", "blackrock inc", "blackrock asset management", "blackrock fund")));
        CORPORATE_DOMAINS.put("vanguard.com", new HashSet<>(Arrays.asList(
            "vanguard", "vanguard group", "vanguard investments")));
        CORPORATE_DOMAINS.put("fidelity.com", new HashSet<>(Arrays.asList(
            "fidelity", "fidelity investments", "fidelity management", "fmr")));
        CORPORATE_DOMAINS.put("goldmansachs.com", new HashSet<>(Arrays.asList(
            "goldman sachs", "gs", "gsam", "goldman sachs asset management")));
        CORPORATE_DOMAINS.put("jpmorgan.com", new HashSet<>(Arrays.asList(
            "jp morgan", "jpmorgan", "jpmc", "jp morgan asset management", "jpm")));
        CORPORATE_DOMAINS.put("morganstanley.com", new HashSet<>(Arrays.asList(
            "morgan stanley", "ms", "morgan stanley investment management", "msim")));
        CORPORATE_DOMAINS.put("ubs.com", new HashSet<>(Arrays.asList(
            "ubs", "ubs asset management", "ubs global", "ubs ag")));
        CORPORATE_DOMAINS.put("credit-suisse.com", new HashSet<>(Arrays.asList(
            "credit suisse", "cs", "credit suisse asset management")));
        CORPORATE_DOMAINS.put("db.com", new HashSet<>(Arrays.asList(
            "deutsche bank", "db", "deutsche asset management", "dws")));
        CORPORATE_DOMAINS.put("barclays.com", new HashSet<>(Arrays.asList(
            "barclays", "barclays capital", "barclays investment")));
        CORPORATE_DOMAINS.put("citi.com", new HashSet<>(Arrays.asList(
            "citigroup", "citi", "citibank", "citigroup global")));
        CORPORATE_DOMAINS.put("hsbc.com", new HashSet<>(Arrays.asList(
            "hsbc", "hsbc global", "hsbc asset management")));
        CORPORATE_DOMAINS.put("statestreet.com", new HashSet<>(Arrays.asList(
            "state street", "state street global", "ssga")));
        CORPORATE_DOMAINS.put("bnymellon.com", new HashSet<>(Arrays.asList(
            "bny mellon", "bank of new york mellon", "bnym")));
        CORPORATE_DOMAINS.put("pimco.com", new HashSet<>(Arrays.asList(
            "pimco", "pacific investment management")));
    }
    
    /**
     * Enhance match score based on email domain evidence
     */
    public void enhance(MatchResult match, String emailDomain) {
        if (emailDomain == null || match.getMatchedEntity() == null) {
            return;
        }
        
        LoanIQEntity entity = match.getMatchedEntity();
        double boost = calculateDomainBoost(emailDomain, entity);
        
        if (boost > 0) {
            double newScore = Math.min(match.getScore() + boost, 100);
            match.setScore(newScore);
            match.addScoreComponent("email_domain_boost", boost);
            
            logger.debug("Enhanced match for {} with email domain {} (boost: {})",
                entity.getFullName(), emailDomain, boost);
        }
    }
    
    private double calculateDomainBoost(String emailDomain, LoanIQEntity entity) {
        double boost = 0;
        
        String entityName = entity.getFullName() != null ?
            entity.getFullName().toLowerCase() : "";
        String fundManager = entity.getUltimateParent() != null ?
            entity.getUltimateParent().toLowerCase() : "";
        
        // Direct domain match
        String domainRoot = extractDomainRoot(emailDomain);
        if (entityName.contains(domainRoot) || fundManager.contains(domainRoot)) {
            boost = 20;  // Higher weight for fund manager matching
            logger.debug("Direct domain match: {} in entity names", domainRoot);
            return boost;
        }
        
        // Corporate family match
        Set<String> corporateNames = CORPORATE_DOMAINS.get(emailDomain);
        if (corporateNames != null) {
            for (String corpName : corporateNames) {
                if (entityName.contains(corpName) || fundManager.contains(corpName)) {
                    boost = 15;
                    logger.debug("Corporate family match: {} via domain {}", corpName, emailDomain);
                    return boost;
                }
            }
        }
        
        // Geographic consistency check
        if (entity.getCountryCode() != null) {
            if (isGeographicMatch(entity.getCountryCode(), emailDomain)) {
                boost = 5;
                logger.debug("Geographic consistency for domain: {}", emailDomain);
            }
        }
        
        // Industry-specific domain patterns
        if (isFinancialDomain(emailDomain) && isFinancialEntity(entity)) {
            boost += 3;
            logger.debug("Financial domain pattern match");
        }
        
        return boost;
    }
    
    private String extractDomainRoot(String emailDomain) {
        if (emailDomain == null) {
            return "";
        }
        
        // Remove TLD and get the main part
        int lastDot = emailDomain.lastIndexOf('.');
        if (lastDot > 0) {
            String withoutTld = emailDomain.substring(0, lastDot);
            // Handle subdomains
            int secondLastDot = withoutTld.lastIndexOf('.');
            if (secondLastDot > 0) {
                return withoutTld.substring(secondLastDot + 1);
            }
            return withoutTld;
        }
        return emailDomain;
    }
    
    private boolean isGeographicMatch(String countryCode, String emailDomain) {
        // Check TLD for country match
        if (emailDomain.endsWith(".uk") && countryCode.equals("GB")) return true;
        if (emailDomain.endsWith(".ca") && countryCode.equals("CA")) return true;
        if (emailDomain.endsWith(".de") && countryCode.equals("DE")) return true;
        if (emailDomain.endsWith(".fr") && countryCode.equals("FR")) return true;
        if (emailDomain.endsWith(".au") && countryCode.equals("AU")) return true;
        if (emailDomain.endsWith(".jp") && countryCode.equals("JP")) return true;
        if (emailDomain.endsWith(".cn") && countryCode.equals("CN")) return true;
        if (emailDomain.endsWith(".sg") && countryCode.equals("SG")) return true;
        if (emailDomain.endsWith(".hk") && countryCode.equals("HK")) return true;
        if (emailDomain.endsWith(".ch") && countryCode.equals("CH")) return true;
        if (emailDomain.endsWith(".nl") && countryCode.equals("NL")) return true;
        if (emailDomain.endsWith(".ie") && countryCode.equals("IE")) return true;
        if (emailDomain.endsWith(".lu") && countryCode.equals("LU")) return true;
        
        // US domains typically use .com
        if (emailDomain.endsWith(".com") && countryCode.equals("US")) return true;
        
        return false;
    }
    
    private boolean isFinancialDomain(String domain) {
        String[] financialKeywords = {
            "bank", "capital", "asset", "invest", "fund", "wealth",
            "securities", "financial", "equity", "credit", "trading"
        };
        
        String domainLower = domain.toLowerCase();
        for (String keyword : financialKeywords) {
            if (domainLower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isFinancialEntity(LoanIQEntity entity) {
        String fullName = (entity.getFullName() != null ? entity.getFullName() : "").toLowerCase();
        String ultimateParent = (entity.getUltimateParent() != null ? entity.getUltimateParent() : "").toLowerCase();
        
        return isFinancialDomain(fullName) || isFinancialDomain(ultimateParent);
    }
}