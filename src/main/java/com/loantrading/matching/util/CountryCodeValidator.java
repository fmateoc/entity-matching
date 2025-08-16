package com.loantrading.matching.util;

import java.util.*;

/**
 * Validates and normalizes country codes
 */
public class CountryCodeValidator {
    
    // ISO 3166-1 alpha-2 country codes
    private static final Set<String> ISO_COUNTRY_CODES = new HashSet<>(Arrays.asList(
        "AD", "AE", "AF", "AG", "AI", "AL", "AM", "AO", "AQ", "AR", "AS", "AT",
        "AU", "AW", "AX", "AZ", "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI",
        "BJ", "BL", "BM", "BN", "BO", "BQ", "BR", "BS", "BT", "BV", "BW", "BY",
        "BZ", "CA", "CC", "CD", "CF", "CG", "CH", "CI", "CK", "CL", "CM", "CN",
        "CO", "CR", "CU", "CV", "CW", "CX", "CY", "CZ", "DE", "DJ", "DK", "DM",
        "DO", "DZ", "EC", "EE", "EG", "EH", "ER", "ES", "ET", "FI", "FJ", "FK",
        "FM", "FO", "FR", "GA", "GB", "GD", "GE", "GF", "GG", "GH", "GI", "GL",
        "GM", "GN", "GP", "GQ", "GR", "GS", "GT", "GU", "GW", "GY", "HK", "HM",
        "HN", "HR", "HT", "HU", "ID", "IE", "IL", "IM", "IN", "IO", "IQ", "IR",
        "IS", "IT", "JE", "JM", "JO", "JP", "KE", "KG", "KH", "KI", "KM", "KN",
        "KP", "KR", "KW", "KY", "KZ", "LA", "LB", "LC", "LI", "LK", "LR", "LS",
        "LT", "LU", "LV", "LY", "MA", "MC", "MD", "ME", "MF", "MG", "MH", "MK",
        "ML", "MM", "MN", "MO", "MP", "MQ", "MR", "MS", "MT", "MU", "MV", "MW",
        "MX", "MY", "MZ", "NA", "NC", "NE", "NF", "NG", "NI", "NL", "NO", "NP",
        "NR", "NU", "NZ", "OM", "PA", "PE", "PF", "PG", "PH", "PK", "PL", "PM",
        "PN", "PR", "PS", "PT", "PW", "PY", "QA", "RE", "RO", "RS", "RU", "RW",
        "SA", "SB", "SC", "SD", "SE", "SG", "SH", "SI", "SJ", "SK", "SL", "SM",
        "SN", "SO", "SR", "SS", "ST", "SV", "SX", "SY", "SZ", "TC", "TD", "TF",
        "TG", "TH", "TJ", "TK", "TL", "TM", "TN", "TO", "TR", "TT", "TV", "TW",
        "TZ", "UA", "UG", "UM", "US", "UY", "UZ", "VA", "VC", "VE", "VG", "VI",
        "VN", "VU", "WF", "WS", "YE", "YT", "ZA", "ZM", "ZW"
    ));
    
    private static final Map<String, String> COUNTRY_NAME_TO_CODE = new HashMap<>();
    
    static {
        // Common country name mappings
        COUNTRY_NAME_TO_CODE.put("UNITED STATES", "US");
        COUNTRY_NAME_TO_CODE.put("USA", "US");
        COUNTRY_NAME_TO_CODE.put("AMERICA", "US");
        COUNTRY_NAME_TO_CODE.put("UNITED STATES OF AMERICA", "US");
        COUNTRY_NAME_TO_CODE.put("UNITED KINGDOM", "GB");
        COUNTRY_NAME_TO_CODE.put("UK", "GB");
        COUNTRY_NAME_TO_CODE.put("ENGLAND", "GB");
        COUNTRY_NAME_TO_CODE.put("GREAT BRITAIN", "GB");
        COUNTRY_NAME_TO_CODE.put("CANADA", "CA");
        COUNTRY_NAME_TO_CODE.put("GERMANY", "DE");
        COUNTRY_NAME_TO_CODE.put("FRANCE", "FR");
        COUNTRY_NAME_TO_CODE.put("JAPAN", "JP");
        COUNTRY_NAME_TO_CODE.put("CHINA", "CN");
        COUNTRY_NAME_TO_CODE.put("PEOPLE'S REPUBLIC OF CHINA", "CN");
        COUNTRY_NAME_TO_CODE.put("AUSTRALIA", "AU");
        COUNTRY_NAME_TO_CODE.put("NETHERLANDS", "NL");
        COUNTRY_NAME_TO_CODE.put("HOLLAND", "NL");
        COUNTRY_NAME_TO_CODE.put("SWITZERLAND", "CH");
        COUNTRY_NAME_TO_CODE.put("SINGAPORE", "SG");
        COUNTRY_NAME_TO_CODE.put("HONG KONG", "HK");
        COUNTRY_NAME_TO_CODE.put("IRELAND", "IE");
        COUNTRY_NAME_TO_CODE.put("LUXEMBOURG", "LU");
        COUNTRY_NAME_TO_CODE.put("CAYMAN ISLANDS", "KY");
        COUNTRY_NAME_TO_CODE.put("BERMUDA", "BM");
        COUNTRY_NAME_TO_CODE.put("BRITISH VIRGIN ISLANDS", "VG");
        COUNTRY_NAME_TO_CODE.put("ISLE OF MAN", "IM");
        COUNTRY_NAME_TO_CODE.put("JERSEY", "JE");
        COUNTRY_NAME_TO_CODE.put("GUERNSEY", "GG");
        COUNTRY_NAME_TO_CODE.put("SOUTH KOREA", "KR");
        COUNTRY_NAME_TO_CODE.put("KOREA", "KR");
        COUNTRY_NAME_TO_CODE.put("INDIA", "IN");
        COUNTRY_NAME_TO_CODE.put("BRAZIL", "BR");
        COUNTRY_NAME_TO_CODE.put("MEXICO", "MX");
        COUNTRY_NAME_TO_CODE.put("SPAIN", "ES");
        COUNTRY_NAME_TO_CODE.put("ITALY", "IT");
        COUNTRY_NAME_TO_CODE.put("SWEDEN", "SE");
        COUNTRY_NAME_TO_CODE.put("NORWAY", "NO");
        COUNTRY_NAME_TO_CODE.put("DENMARK", "DK");
        COUNTRY_NAME_TO_CODE.put("FINLAND", "FI");
        COUNTRY_NAME_TO_CODE.put("BELGIUM", "BE");
        COUNTRY_NAME_TO_CODE.put("AUSTRIA", "AT");
        COUNTRY_NAME_TO_CODE.put("PORTUGAL", "PT");
    }
    
    /**
     * Check if a country code is valid
     */
    public boolean isValidCountryCode(String code) {
        return code != null && ISO_COUNTRY_CODES.contains(code.toUpperCase());
    }
    
    /**
     * Normalize country name or code to ISO code
     */
    public String normalizeCountry(String country) {
        if (country == null) {
            return null;
        }
        
        country = country.trim().toUpperCase();
        
        // If already a valid code, return it
        if (country.length() == 2 && ISO_COUNTRY_CODES.contains(country)) {
            return country;
        }
        
        // Try to map from country name
        return COUNTRY_NAME_TO_CODE.getOrDefault(country, country);
    }
    
    /**
     * Check if MEI country code matches address country
     */
    public boolean isGeographicMatch(String meiCountryCode, String addressCountry) {
        if (meiCountryCode == null || addressCountry == null) {
            return false;
        }
        
        String normalizedAddress = normalizeCountry(addressCountry);
        return meiCountryCode.equals(normalizedAddress);
    }
}