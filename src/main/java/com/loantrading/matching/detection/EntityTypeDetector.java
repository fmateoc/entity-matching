package com.loantrading.matching.detection;

import com.loantrading.matching.entity.EntityType;
import com.loantrading.matching.entity.ExtractedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Detects whether an entity is a managed fund or standalone entity
 */
public class EntityTypeDetector {
    private static final Logger logger = LoggerFactory.getLogger(EntityTypeDetector.class);
    
    private static final Set<String> FUND_MANAGER_INDICATORS = new HashSet<>(Arrays.asList(
        "asset management", "capital management", "investment management",
        "advisors", "advisers", "partners", "holdings", "investments", "ventures",
        "equity", "credit", "securities", "wealth", "advisory", "capital",
        "funds", "portfolio", "strategies"
    ));
    
    private static final Set<String> STANDALONE_INDICATORS = new HashSet<>(Arrays.asList(
        "corporation", "bank", "insurance", "manufacturing", "retail",
        "technology", "pharmaceutical", "energy", "utilities", "telecom",
        "mining", "construction", "logistics", "shipping", "airline"
    ));
    
    private static final Set<String> INSTITUTIONAL_INVESTOR_PATTERNS = new HashSet<>(Arrays.asList(
        "pension", "endowment", "retirement", "foundation", "trust",
        "university", "college", "charity", "sovereign wealth",
        "superannuation", "provident", "social security", "teachers",
        "employees", "workers", "municipal", "state of", "county of"
    ));
    
    private static final Map<String, EntityType> KNOWN_FUND_MANAGER_DOMAINS = new HashMap<>();
    
    static {
        // Known fund manager domains
        KNOWN_FUND_MANAGER_DOMAINS.put("blackrock.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("vanguard.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("fidelity.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("pimco.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("goldmansachs.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("jpmorgan.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("morganstanley.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("ubs.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("credit-suisse.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("barclays.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("statestreet.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("alliancebernstein.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("bnpparibas.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("axa-im.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("schroders.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("wellington.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("troweprice.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("franklintempleton.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("invesco.com", EntityType.MANAGED_FUND);
        KNOWN_FUND_MANAGER_DOMAINS.put("dimensional.com", EntityType.MANAGED_FUND);
    }
    
    /**
     * Detect the entity type based on extracted data
     */
    public EntityType detectType(ExtractedEntity entity) {
        List<TypeSignal> signals = new ArrayList<>();
        
        // Check if fund manager field is explicitly present
        if (entity.getFundManager() != null && !entity.getFundManager().isEmpty()) {
            signals.add(new TypeSignal(EntityType.MANAGED_FUND, 0.95,
                "Fund manager field explicitly present: " + entity.getFundManager()));
        }
        
        // Analyze legal name patterns
        if (entity.getLegalName() != null) {
            analyzeNamePatterns(entity.getLegalName(), signals);
        }
        
        // Check email domain
        if (entity.getEmailDomain() != null) {
            analyzeEmailDomain(entity.getEmailDomain(), signals);
        }
        
        // Check for institutional investor patterns
        if (entity.getLegalName() != null) {
            checkInstitutionalPatterns(entity.getLegalName(), signals);
        }
        
        // Check short name hints from raw fields
        String shortName = entity.getRawFields().get("short_name");
        if (shortName != null) {
            analyzeShortName(shortName, signals);
        }
        
        // Aggregate signals to determine type
        EntityType result = aggregateSignals(signals);
        
        logger.info("Entity type detected as {} with {} signals", result, signals.size());
        logTopSignals(signals);
        
        return result;
    }
    
    private void analyzeNamePatterns(String name, List<TypeSignal> signals) {
        String nameLower = name.toLowerCase();
        
        // Check for fund manager indicators
        for (String indicator : FUND_MANAGER_INDICATORS) {
            if (nameLower.contains(indicator)) {
                signals.add(new TypeSignal(EntityType.MANAGED_FUND, 0.75,
                    "Fund manager indicator in name: " + indicator));
                break; // One strong signal is enough
            }
        }
        
        // Check for standalone indicators
        for (String indicator : STANDALONE_INDICATORS) {
            if (nameLower.contains(indicator) && !nameLower.contains("fund") && 
                !nameLower.contains("investment") && !nameLower.contains("management")) {
                signals.add(new TypeSignal(EntityType.STANDALONE, 0.65,
                    "Standalone indicator in name: " + indicator));
                break;
            }
        }
    }
    
    private void analyzeEmailDomain(String domain, List<TypeSignal> signals) {
        EntityType knownType = KNOWN_FUND_MANAGER_DOMAINS.get(domain);
        if (knownType != null) {
            signals.add(new TypeSignal(knownType, 0.85,
                "Known fund manager email domain: " + domain));
        }
        
        // Check domain patterns
        if (domain.contains("asset") || domain.contains("capital") ||
            domain.contains("invest") || domain.contains("fund") ||
            domain.contains("wealth") || domain.contains("advisory")) {
            signals.add(new TypeSignal(EntityType.MANAGED_FUND, 0.7,
                "Fund management pattern in email domain: " + domain));
        }
    }
    
    private void checkInstitutionalPatterns(String name, List<TypeSignal> signals) {
        String nameLower = name.toLowerCase();
        
        for (String pattern : INSTITUTIONAL_INVESTOR_PATTERNS) {
            if (nameLower.contains(pattern)) {
                signals.add(new TypeSignal(EntityType.MANAGED_FUND, 0.8,
                    "Institutional investor pattern: " + pattern));
                return;
            }
        }
    }
    
    private void analyzeShortName(String shortName, List<TypeSignal> signals) {
        String upperShort = shortName.toUpperCase();
        
        // Check for FM suffix or other fund manager indicators
        if (upperShort.endsWith("FM") || upperShort.endsWith("_FM") ||
            upperShort.contains("_FM_") || upperShort.contains("-FM-") ||
            upperShort.endsWith("FUND") || upperShort.contains("MGMT")) {
            signals.add(new TypeSignal(EntityType.MANAGED_FUND, 0.7,
                "Fund manager indicator in short name: " + shortName));
        }
    }
    
    private EntityType aggregateSignals(List<TypeSignal> signals) {
        if (signals.isEmpty()) {
            return EntityType.UNKNOWN;
        }
        
        // Weight signals by confidence
        Map<EntityType, Double> scores = new HashMap<>();
        Map<EntityType, Integer> counts = new HashMap<>();
        
        for (TypeSignal signal : signals) {
            scores.merge(signal.type, signal.confidence, Double::sum);
            counts.merge(signal.type, 1, Integer::sum);
        }
        
        // Find highest weighted score
        EntityType bestType = EntityType.UNKNOWN;
        double bestScore = 0;
        
        for (Map.Entry<EntityType, Double> entry : scores.entrySet()) {
            // Use average score weighted by count
            double weightedScore = entry.getValue() / Math.sqrt(counts.get(entry.getKey()));
            if (weightedScore > bestScore) {
                bestScore = weightedScore;
                bestType = entry.getKey();
            }
        }
        
        // Require minimum confidence threshold
        if (bestScore < 0.5) {
            return EntityType.UNKNOWN;
        }
        
        return bestType;
    }
    
    private void logTopSignals(List<TypeSignal> signals) {
        if (logger.isDebugEnabled() && !signals.isEmpty()) {
            signals.sort((a, b) -> Double.compare(b.confidence, a.confidence));
            int count = Math.min(3, signals.size());
            for (int i = 0; i < count; i++) {
                TypeSignal signal = signals.get(i);
                logger.debug("  Signal {}: {} (confidence: {})", 
                    i + 1, signal.reason, signal.confidence);
            }
        }
    }
    
    /**
     * Internal class to represent a type detection signal
     */
    private static class TypeSignal {
        final EntityType type;
        final double confidence;
        final String reason;
        
        TypeSignal(EntityType type, double confidence, String reason) {
            this.type = type;
            this.confidence = confidence;
            this.reason = reason;
        }
    }
}