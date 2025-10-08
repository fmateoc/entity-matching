package com.loantrading.matching.discrepancy;

import com.loantrading.matching.entity.Discrepancy;
import com.loantrading.matching.entity.DiscrepancySeverity;
import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.MatchedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Detects discrepancies between an extracted entity and a matched candidate entity.
 */
public class DiscrepancyDetector {

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

        // Add other checks as needed, e.g., for address, country code, etc.

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