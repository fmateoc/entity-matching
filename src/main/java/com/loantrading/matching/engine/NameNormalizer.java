package com.loantrading.matching.engine;

import com.loantrading.matching.extraction.CharacterNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes entity names for comparison by applying general character normalization
 * and then specific business rules for names.
 */
public class NameNormalizer {
    private static final Logger logger = LoggerFactory.getLogger(NameNormalizer.class);
    private final CharacterNormalizer characterNormalizer;
    
    // Comprehensive corporate forms
    private static final Set<String> CORPORATE_FORMS = new HashSet<>(Arrays.asList(
        // US forms
        "inc", "incorporated", "corp", "corporation", "llc", "llp", "lp",
        "ltd", "limited", "co", "company", "holding", "holdings",
        "enterprises", "ent", "industries", "ind",
        
        // International forms
        "plc", "sa", "ag", "gmbh", "bv", "nv", "spa", "srl", "sarl",
        "ab", "as", "oy", "pty", "pte", "bhd", "sdn", "tbk", "pt",
        "kk", "kg", "kft", "sp zoo", "doo", "ad", "ead", "ooo", "zao",
        "ltda", "cv", "sas", "scs", "snc", "kgaa", "gmbh co kg",
        
        // Investment specific
        "fund", "funds", "trust", "reit", "sicav", "sicaf", "fcp",
        "partners", "partnership", "investments", "capital", "ventures",
        "equity", "credit", "asset", "management", "advisors", "advisers"
    ));
    private static final Pattern CORPORATE_FORMS_PATTERN;
    private static final Map<String, String> ABBREVIATIONS = new HashMap<>();
    
    static {
        // Build the corporate forms regex pattern
        String formsRegex = String.join("|", CORPORATE_FORMS);
        CORPORATE_FORMS_PATTERN = Pattern.compile("\\b(" + formsRegex + ")\\b", Pattern.CASE_INSENSITIVE);

        // Common abbreviations
        ABBREVIATIONS.put("intl", "international");
        ABBREVIATIONS.put("natl", "national");
        ABBREVIATIONS.put("mgmt", "management");
        ABBREVIATIONS.put("invt", "investment");
        ABBREVIATIONS.put("svcs", "services");
        ABBREVIATIONS.put("svc", "service");
        ABBREVIATIONS.put("tech", "technology");
        ABBREVIATIONS.put("assoc", "associates");
        ABBREVIATIONS.put("bros", "brothers");
        ABBREVIATIONS.put("dept", "department");
        ABBREVIATIONS.put("div", "division");
        ABBREVIATIONS.put("govt", "government");
        ABBREVIATIONS.put("univ", "university");
        ABBREVIATIONS.put("mfg", "manufacturing");
        ABBREVIATIONS.put("ins", "insurance");
        ABBREVIATIONS.put("fin", "financial");
        ABBREVIATIONS.put("grp", "group");
        ABBREVIATIONS.put("sys", "systems");
        ABBREVIATIONS.put("amer", "american");
        ABBREVIATIONS.put("euro", "european");
        ABBREVIATIONS.put("asia", "asian");
        ABBREVIATIONS.put("pac", "pacific");
        ABBREVIATIONS.put("atl", "atlantic");
    }
    
    // Fund manager specific mappings
    private static final Map<String, String> FUND_MANAGER_ALIASES = new HashMap<>();
    
    static {
        // Common fund manager aliases
        FUND_MANAGER_ALIASES.put("gsam", "goldman sachs asset management");
        FUND_MANAGER_ALIASES.put("gs", "goldman sachs");
        FUND_MANAGER_ALIASES.put("jpm", "jp morgan");
        FUND_MANAGER_ALIASES.put("jpmc", "jp morgan chase");
        FUND_MANAGER_ALIASES.put("ms", "morgan stanley");
        FUND_MANAGER_ALIASES.put("msim", "morgan stanley investment management");
        FUND_MANAGER_ALIASES.put("baml", "bank of america merrill lynch");
        FUND_MANAGER_ALIASES.put("bofa", "bank of america");
        FUND_MANAGER_ALIASES.put("ubs", "ubs asset management");
        FUND_MANAGER_ALIASES.put("cs", "credit suisse");
        FUND_MANAGER_ALIASES.put("db", "deutsche bank");
        FUND_MANAGER_ALIASES.put("dws", "deutsche wealth management");
        FUND_MANAGER_ALIASES.put("ssga", "state street global advisors");
        FUND_MANAGER_ALIASES.put("bny", "bank of new york");
        FUND_MANAGER_ALIASES.put("bnym", "bank of new york mellon");
        FUND_MANAGER_ALIASES.put("citi", "citigroup");
        FUND_MANAGER_ALIASES.put("hsbc", "hsbc global");
        FUND_MANAGER_ALIASES.put("bnp", "bnp paribas");
        FUND_MANAGER_ALIASES.put("axa", "axa investment");
        FUND_MANAGER_ALIASES.put("ab", "alliancebernstein");
        FUND_MANAGER_ALIASES.put("pimco", "pacific investment management company");
        FUND_MANAGER_ALIASES.put("blackrock", "blackrock inc");
        FUND_MANAGER_ALIASES.put("vanguard", "vanguard group");
    }

    public NameNormalizer() {
        this.characterNormalizer = new CharacterNormalizer();
    }
    
    /**
     * Normalize a general entity name
     */
    public String normalize(String name) {
        if (name == null) {
            return "";
        }
        
        // 1. Perform general character-level normalization (handles diacritics, smart quotes, etc.)
        String normalized = characterNormalizer.normalizeUnicodeAndPunctuation(name);

        // 2. Convert to lowercase for case-insensitive matching.
        normalized = normalized.toLowerCase();
        
        // 3. Filter to the character set required for name matching (removes quotes, etc.)
        normalized = normalized.replaceAll("[^a-z0-9\\s-']", " ");
        
        // 4. Expand common abbreviations specific to names
        for (Map.Entry<String, String> abbr : ABBREVIATIONS.entrySet()) {
            normalized = normalized.replaceAll("\\b" + abbr.getKey() + "\\b", abbr.getValue());
        }
        
        // 5. Remove corporate forms
        normalized = CORPORATE_FORMS_PATTERN.matcher(normalized).replaceAll("");
        
        // 6. Remove articles
        normalized = normalized.replaceAll("\\b(the|a|an|and|of|in|for|by|with|from)\\b", "");
        
        // 7. Final whitespace cleanup
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized;
    }
    
    /**
     * Normalize a fund manager name with special handling
     */
    public String normalizeFundManager(String name) {
        if (name == null) {
            return "";
        }
        
        // First apply general normalization
        String normalized = normalize(name);
        
        // Check for known aliases (exact match after normalization)
        String alias = FUND_MANAGER_ALIASES.get(normalized);
        if (alias != null) {
            return alias;
        }
        
        // Check if normalized name contains or is contained in any known fund manager
        for (Map.Entry<String, String> entry : FUND_MANAGER_ALIASES.entrySet()) {
            String fullName = entry.getValue();
            if (normalized.contains(fullName) || fullName.contains(normalized)) {
                return fullName;
            }
        }
        
        return normalized;
    }
    
    /**
     * Extract and normalize DBA (Doing Business As) from a name
     */
    public DBAComponents extractDBA(String fullName) {
        if (fullName == null) {
            return new DBAComponents(null, null);
        }
        
        // Look for DBA patterns
        String[] patterns = {"DBA", "d/b/a", "d.b.a.", "trading as", "t/a"};
        
        for (String pattern : patterns) {
            String regex = "(?i)(.+?)\\s+" + Pattern.quote(pattern) + "\\s+(.+)";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(fullName);
            
            if (m.find()) {
                String legalName = m.group(1).trim();
                String tradeName = m.group(2).trim();
                return new DBAComponents(legalName, tradeName);
            }
        }
        
        return new DBAComponents(fullName, null);
    }
    
    /**
     * Container for DBA components
     */
    public static class DBAComponents {
        public final String legalName;
        public final String tradeName;
        
        public DBAComponents(String legalName, String tradeName) {
            this.legalName = legalName;
            this.tradeName = tradeName;
        }
        
        public boolean hasDBA() {
            return tradeName != null;
        }
    }
}