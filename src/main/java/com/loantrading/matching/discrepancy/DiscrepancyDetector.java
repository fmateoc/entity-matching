package com.loantrading.matching.discrepancy;

import com.loantrading.matching.entity.Discrepancy;
import com.loantrading.matching.entity.DiscrepancySeverity;
import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.MatchedEntity;
import com.loantrading.matching.framework.preprocessor.NameNormalizer;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Detects discrepancies between an extracted entity and a matched candidate entity.
 */
public class DiscrepancyDetector {

    private final NameNormalizer nameNormalizer;

    public DiscrepancyDetector(NameNormalizer nameNormalizer) {
        this.nameNormalizer = nameNormalizer;
    }

    /**
     * Compares the source and target entities and returns a list of any discrepancies found.
     *
     * @param source The entity extracted from the source document.
     * @param target The candidate entity from the database that was matched.
     * @return A list of Discrepancy objects.
     */
    public List<Discrepancy> detect(ExtractedEntity source, MatchedEntity target) {
        List<Discrepancy> discrepancies = new ArrayList<>();

        // Check for EIN mismatch (Critical)
        compareFields(
            discrepancies,
            "EIN",
            source.getEin(),
            target.getEin(),
            "EIN mismatch between source document and matched entity.",
            DiscrepancySeverity.CRITICAL
        );

        // Check for LEI mismatch (High)
        compareFields(
            discrepancies,
            "LEI",
            source.getLei(),
            target.getLei(),
            "LEI mismatch between source document and matched entity.",
            DiscrepancySeverity.HIGH
        );

        return discrepancies;
    }

    /**
     * Compares fields between two source documents (e.g., ADF and Tax Form) to find discrepancies.
     * This now uses a two-stage check: first for corporate forms, then a fuzzy match on the core name.
     *
     * @param adfEntity The entity extracted from the Admin Details Form.
     * @param taxFormEntity The entity extracted from the Tax Form.
     * @return A list of Discrepancy objects.
     */
    public List<Discrepancy> detectCrossDocumentDiscrepancies(ExtractedEntity adfEntity, ExtractedEntity taxFormEntity) {
        List<Discrepancy> discrepancies = new ArrayList<>();

        if (adfEntity.getRawLegalName() == null || taxFormEntity.getRawLegalName() == null) {
            return discrepancies;
        }

        NameNormalizer.NameComponents adfComponents = nameNormalizer.extractNameComponents(adfEntity.getRawLegalName());
        NameNormalizer.NameComponents taxComponents = nameNormalizer.extractNameComponents(taxFormEntity.getRawLegalName());

        // Stage 1: Compare corporate forms. A mismatch here is a critical discrepancy.
        if (!Objects.equals(adfComponents.corporateForm, taxComponents.corporateForm)) {
            Discrepancy discrepancy = new Discrepancy(
                "corporateForm",
                DiscrepancySeverity.CRITICAL,
                "Corporate form differs between ADF and Tax Form."
            );
            discrepancy.addDetail("adfCorporateForm", adfComponents.corporateForm);
            discrepancy.addDetail("taxCorporateForm", taxComponents.corporateForm);
            discrepancies.add(discrepancy);
            return discrepancies; // Return immediately as this is a major issue.
        }

        // Stage 2: If corporate forms match, do a fuzzy compare on the core names to allow for OCR noise.
        LevenshteinDistance levenshtein = new LevenshteinDistance();
        String normalizedAdfCore = nameNormalizer.normalize(adfComponents.coreName);
        String normalizedTaxCore = nameNormalizer.normalize(taxComponents.coreName);

        int distance = levenshtein.apply(normalizedAdfCore, normalizedTaxCore);
        int maxLength = Math.max(normalizedAdfCore.length(), normalizedTaxCore.length());
        double similarity = (maxLength > 0) ? (double) (maxLength - distance) / maxLength : 1.0;

        // Only flag a discrepancy if the core names are significantly different.
        if (similarity < 0.90) {
            Discrepancy discrepancy = new Discrepancy(
                "coreName",
                DiscrepancySeverity.MEDIUM, // A difference in the core name is less severe than a corporate form mismatch
                "Core name differs significantly between ADF and Tax Form."
            );
            discrepancy.addDetail("adfCoreName", adfComponents.coreName);
            discrepancy.addDetail("taxCoreName", taxComponents.coreName);
            discrepancy.addDetail("similarityScore", String.format("%.2f", similarity));
            discrepancies.add(discrepancy);
        }


        return discrepancies;
    }

    private void compareFields(List<Discrepancy> discrepancies, String type, String sourceValue, String targetValue, String description, DiscrepancySeverity severity) {
        // Only compare if the source value is not null. A null source value isn't a discrepancy.
        if (sourceValue != null && !Objects.equals(sourceValue, targetValue)) {
            Discrepancy discrepancy = new Discrepancy(type, severity, description);
            discrepancy.addDetail("sourceValue", sourceValue);
            discrepancy.addDetail("targetValue", targetValue != null ? targetValue : "null");
            discrepancies.add(discrepancy);
        }
    }
}