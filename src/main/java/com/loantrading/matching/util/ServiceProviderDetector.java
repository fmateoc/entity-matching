package com.loantrading.matching.util;

import java.util.*;

/**
 * Detects and filters service provider email domains
 */
public class ServiceProviderDetector {
    
    // Comprehensive list of service provider domains to filter out
    private static final Set<String> SERVICE_PROVIDER_DOMAINS = new HashSet<>(Arrays.asList(
        // Generic email providers
        "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "aol.com",
        "protonmail.com", "mail.com", "yandex.com", "icloud.com", "msn.com",
        "live.com", "me.com", "qq.com", "163.com", "126.com", "yeah.net",
        
        // Law firms (major international)
        "shearman.com", "davispolk.com", "sullcrom.com", "weil.com",
        "skadden.com", "lw.com", "kirkland.com", "paulweiss.com",
        "cooley.com", "wilmerhale.com", "mayerbrown.com", "whitecase.com",
        "cliffordchance.com", "linklaters.com", "allenovery.com",
        "freshfields.com", "hoganlovells.com", "nortonrosefulbright.com",
        "dechert.com", "sidley.com", "morganlewis.com", "jonesday.com",
        "gibsondunn.com", "cravath.com", "wachtell.com", "simpson.com",
        
        // Fund administrators
        "citco.com", "sscinc.com", "maples.com", "intertrust.com",
        "alterDomus.com", "apexgroup.com", "aztecgroup.com", "tmf-group.com",
        "vistra.com", "tridenttrust.com", "iqeq.com", "jcftrust.com",
        "harneys.com", "ogier.com", "walkers.global", "mourant.com",
        
        // Custodians
        "bnymellon.com", "statestreet.com", "northerntrust.com",
        "jpmorgan.com/custody", "citi.com/custody", "hsbc.com/custody",
        "standardchartered.com/custody", "db.com/custody",
        
        // Accounting firms
        "pwc.com", "deloitte.com", "ey.com", "kpmg.com",
        "bdo.com", "grantthornton.com", "rsm.com", "mazars.com",
        "bakertilly.com", "crowe.com", "mossadams.com", "marcumllp.com",
        
        // Generic service provider indicators
        "lawfirm.com", "legal.com", "attorneys.com", "lawyers.com",
        "admin.com", "administration.com", "fundadmin.com",
        "custodian.com", "trustee.com", "fiduciary.com"
    ));
    
    // Keywords that indicate service provider domains
    private static final Set<String> SERVICE_PROVIDER_KEYWORDS = new HashSet<>(Arrays.asList(
        "law", "legal", "attorney", "counsel", "llp", "solicitor",
        "admin", "administrator", "custody", "custodian",
        "trustee", "fiduciary", "accounting", "audit", "tax"
    ));
    
    /**
     * Check if a domain belongs to a service provider
     */
    public boolean isServiceProviderDomain(String domain) {
        if (domain == null) {
            return false;
        }
        
        domain = domain.toLowerCase().trim();
        
        // Check exact match
        if (SERVICE_PROVIDER_DOMAINS.contains(domain)) {
            return true;
        }
        
        // Check for partial matches with keywords
        for (String keyword : SERVICE_PROVIDER_KEYWORDS) {
            if (domain.contains(keyword)) {
                return true;
            }
        }
        
        // Check for common law firm patterns
        if (isLawFirmDomain(domain)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if domain appears to be a law firm
     */
    private boolean isLawFirmDomain(String domain) {
        // Common law firm domain patterns
        String[] lawFirmPatterns = {
            ".*law\\.com$",
            ".*legal\\.com$",
            ".*llp\\.com$",
            ".*attorneys\\.com$",
            ".*solicitors\\..*",
            ".*barristers\\..*"
        };
        
        for (String pattern : lawFirmPatterns) {
            if (domain.matches(pattern)) {
                return true;
            }
        }
        
        // Check for ampersand which is common in law firms (e.g., "smith&jones.com")
        if (domain.contains("&") || domain.contains("and")) {
            // Additional check for multiple names
            String[] parts = domain.split("\\.|&|and");
            if (parts.length >= 3) {
                return true; // Likely a law firm with partner names
            }
        }
        
        return false;
    }
    
    /**
     * Filter out service provider emails from a list
     */
    public List<String> filterServiceProviderEmails(List<String> emails) {
        List<String> filtered = new ArrayList<>();
        
        for (String email : emails) {
            String domain = extractDomain(email);
            if (!isServiceProviderDomain(domain)) {
                filtered.add(email);
            }
        }
        
        return filtered;
    }
    
    /**
     * Extract domain from email address
     */
    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        
        return email.substring(email.indexOf('@') + 1).toLowerCase();
    }
    
    /**
     * Find the most likely entity domain from a list of emails
     */
    public String findPrimaryEntityDomain(List<String> emails) {
        Map<String, Integer> domainCounts = new HashMap<>();
        
        for (String email : emails) {
            String domain = extractDomain(email);
            if (domain != null && !isServiceProviderDomain(domain)) {
                domainCounts.merge(domain, 1, Integer::sum);
            }
        }
        
        // Return most frequent non-service provider domain
        return domainCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}