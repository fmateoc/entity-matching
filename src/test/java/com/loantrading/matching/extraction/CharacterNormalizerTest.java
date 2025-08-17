package com.loantrading.matching.extraction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CharacterNormalizerTest {

    private CharacterNormalizer characterNormalizer;

    @BeforeEach
    void setUp() {
        characterNormalizer = new CharacterNormalizer();
    }

    @Test
    @DisplayName("Should replace smart quotes with standard quotes")
    void testSmartQuotes() {
        assertEquals("\"Hello\"", characterNormalizer.normalize("“Hello”"));
        assertEquals("'Quote'", characterNormalizer.normalize("‘Quote’"));
    }

    @Test
    @DisplayName("Should replace various dash characters with a standard hyphen")
    void testDashes() {
        assertEquals("long-running", characterNormalizer.normalize("long—running"));
        assertEquals("a-b", characterNormalizer.normalize("a–b"));
    }

    @Test
    @DisplayName("Should correct common OCR errors")
    void testOCRErrors() {
        assertEquals("modem", characterNormalizer.normalize("modern"));
        assertEquals("10", characterNormalizer.normalize("l0"));
        assertEquals("02", characterNormalizer.normalize("O2"));
        assertEquals("20", characterNormalizer.normalize("2O"));
        assertEquals("21", characterNormalizer.normalize("2l"));
    }

    @Test
    @DisplayName("Should normalize whitespace")
    void testWhitespaceNormalization() {
        assertEquals("a b c", characterNormalizer.normalize("a\u00A0b\u2000c"));
        assertEquals("d e f", characterNormalizer.normalize("d  e\t f"));
    }

    @Test
    @DisplayName("Should clean entity names by removing brackets and quotes")
    void testCleanEntityName() {
        assertEquals("Clean Name", characterNormalizer.cleanEntityName("Clean Name [some note]"));
        assertEquals("Another Name", characterNormalizer.cleanEntityName("Another Name (extra info)"));
        assertEquals("Quoted Name", characterNormalizer.cleanEntityName("\"Quoted Name\""));
    }

    @Test
    @DisplayName("Should handle null and empty strings gracefully")
    void testNullAndEmptyStrings() {
        assertEquals("", characterNormalizer.normalize(null));
        assertEquals("", characterNormalizer.normalize(""));
        assertNull(characterNormalizer.cleanEntityName(null));
        assertEquals("", characterNormalizer.cleanEntityName(""));
    }

    @Test
    @DisplayName("Should remove diacritics from characters")
    void testDiacriticNormalization() {
        assertEquals("cafe", characterNormalizer.normalize("café"));
        assertEquals("uber", characterNormalizer.normalize("über"));
        assertEquals("espanol", characterNormalizer.normalize("español"));
        assertEquals("francois", characterNormalizer.normalize("françois"));
    }
}
