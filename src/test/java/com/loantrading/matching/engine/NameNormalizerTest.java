package com.loantrading.matching.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NameNormalizerTest {

    private NameNormalizer nameNormalizer;

    @BeforeEach
    void setUp() {
        nameNormalizer = new NameNormalizer();
    }

    @Test
    @DisplayName("Should remove standard corporate forms")
    void testRemoveCorporateForms() {
        assertEquals("global technology", nameNormalizer.normalize("Global Tech Inc."));
        assertEquals("creative solutions", nameNormalizer.normalize("Creative Solutions, LLC"));
        assertEquals("international trade", nameNormalizer.normalize("International Trade Co."));
        assertEquals("acme", nameNormalizer.normalize("ACME Holdings PLC"));
    }

    @Test
    @DisplayName("Should expand common abbreviations")
    void testExpandAbbreviations() {
        assertEquals("international business machines", nameNormalizer.normalize("Intl Business Machines"));
        assertEquals("national services", nameNormalizer.normalize("Natl Svcs"));
        assertEquals("financial group", nameNormalizer.normalize("Fin Grp"));
    }

    @Test
    @DisplayName("Should handle special characters and extra spaces")
    void testSpecialCharactersAndSpacing() {
        assertEquals("o'connor associates", nameNormalizer.normalize("O'Connor & Associates"));
        assertEquals("alpha-beta solutions", nameNormalizer.normalize("Alpha-Beta   Solutions"));
        assertEquals("test name numbers 123", nameNormalizer.normalize("Test Name with numbers 123!@#"));
    }

    @Test
    @DisplayName("Should handle complex names with multiple transformations")
    void testComplexNormalization() {
        assertEquals("apex financial services finsvcs", nameNormalizer.normalize("Apex Financial Services, Ltd. (FinSvcs)"));
        assertEquals("international technology", nameNormalizer.normalize("Intl. Tech Industries Inc"));
    }

    @Test
    @DisplayName("Should extract DBA components correctly")
    void testExtractDBA() {
        NameNormalizer.DBAComponents components1 = nameNormalizer.extractDBA("Real Company Inc. d/b/a Fake Company");
        assertEquals("Real Company Inc.", components1.legalName);
        assertEquals("Fake Company", components1.tradeName);
        assertTrue(components1.hasDBA());

        NameNormalizer.DBAComponents components2 = nameNormalizer.extractDBA("Another Corp DBA Awesome Services");
        assertEquals("Another Corp", components2.legalName);
        assertEquals("Awesome Services", components2.tradeName);
        assertTrue(components2.hasDBA());
    }

    @Test
    @DisplayName("Should return original name when no DBA is present")
    void testNoDBA() {
        NameNormalizer.DBAComponents components = nameNormalizer.extractDBA("Just A Regular Company Name");
        assertEquals("Just A Regular Company Name", components.legalName);
        assertNull(components.tradeName);
        assertFalse(components.hasDBA());
    }
}
