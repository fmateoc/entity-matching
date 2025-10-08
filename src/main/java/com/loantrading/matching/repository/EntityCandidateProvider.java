package com.loantrading.matching.repository;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.MatchedEntity;
import com.loantrading.matching.framework.match.ICandidateProvider;
import com.loantrading.matching.framework.workflow.MatchingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides candidate entities from the database based on the extracted entity data.
 * This class encapsulates the database query logic for finding potential matches.
 */
public class EntityCandidateProvider implements ICandidateProvider<MatchedEntity, ExtractedEntity> {

    private static final Logger logger = LoggerFactory.getLogger(EntityCandidateProvider.class);
    private final Connection connection;

    public EntityCandidateProvider(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<MatchedEntity> findCandidates(ExtractedEntity source, MatchingContext context) {
        // This is a simplified version of the candidate retrieval logic that would
        // exist in the real MatchingEngine. It would likely have more complex rules
        // for querying based on different identifiers and names.
        String sql = "SELECT * FROM entities WHERE " +
                     "LOWER(full_name) LIKE ? OR " +
                     "mei = ? OR lei = ? OR ein = ?";

        List<MatchedEntity> candidates = new ArrayList<>();
        String searchName = "%" + (source.getLegalName() != null ? source.getLegalName().toLowerCase() : "___") + "%";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, searchName);
            pstmt.setString(2, source.getMei());
            pstmt.setString(3, source.getLei());
            pstmt.setString(4, source.getEin());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MatchedEntity entity = new MatchedEntity();
                    entity.setEntityId(rs.getLong("entity_id"));
                    entity.setFullName(rs.getString("full_name"));
                    entity.setShortName(rs.getString("short_name"));
                    entity.setUltimateParent(rs.getString("ultimate_parent"));
                    entity.setMei(rs.getString("mei"));
                    entity.setLei(rs.getString("lei"));
                    entity.setEin(rs.getString("ein"));
                    // ... set other fields
                    candidates.add(entity);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve candidate entities", e);
        }

        logger.info("Found {} potential candidates for '{}'", candidates.size(), source.getLegalName());
        return candidates;
    }
}