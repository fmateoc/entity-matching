package com.loantrading.matching.framework.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * A database-backed implementation of the {@link IMatchRepository}.
 *
 * This class uses a JDBC connection to interact with a `persisted_matches`
 * table to store and retrieve confirmed entity matches.
 */
public class DbMatchRepository implements IMatchRepository {

    private static final Logger logger = LoggerFactory.getLogger(DbMatchRepository.class);

    private final Connection connection;

    public DbMatchRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void saveMatch(PersistedMatch match) {
        // Use MERGE (or equivalent UPSERT) to avoid duplicates.
        // If a match for the source type/identifier already exists, this will update it.
        String sql = "MERGE INTO persisted_matches (source_entity_type, source_entity_identifier, target_entity_id, confidence_score, matched_by, last_updated_at) " +
                     "KEY(source_entity_type, source_entity_identifier) " +
                     "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, match.getSourceEntityType());
            pstmt.setString(2, match.getSourceEntityIdentifier());
            pstmt.setLong(3, match.getTargetEntityId());
            pstmt.setDouble(4, match.getConfidenceScore());
            pstmt.setString(5, match.getMatchedBy());
            pstmt.executeUpdate();
            logger.info("Saved/updated persistent match for '{}' -> '{}'", match.getSourceEntityIdentifier(), match.getTargetEntityId());
        } catch (SQLException e) {
            logger.error("Failed to save persistent match", e);
            // In a real application, a more specific exception might be thrown.
        }
    }

    @Override
    public Optional<Long> findMatch(String sourceEntityType, String sourceEntityIdentifier) {
        String sql = "SELECT target_entity_id FROM persisted_matches WHERE source_entity_type = ? AND source_entity_identifier = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sourceEntityType);
            pstmt.setString(2, sourceEntityIdentifier);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long targetId = rs.getLong("target_entity_id");
                    logger.debug("Found persistent match for '{}': {}", sourceEntityIdentifier, targetId);
                    return Optional.of(targetId);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to query for persistent match", e);
        }

        logger.debug("No persistent match found for '{}'", sourceEntityIdentifier);
        return Optional.empty();
    }
}