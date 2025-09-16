package com.loantrading.matching.repository;

import com.loantrading.matching.entity.LoanIQEntity;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Repository for accessing LoanIQ database
 */
public class LoanIQRepository {
    private static final Logger logger = LoggerFactory.getLogger(LoanIQRepository.class);
    
    private final Connection connection;
    private final LoadingCache<String, List<LoanIQEntity>> cache;
    private final Map<String, PreparedStatement> statements;
    
    public LoanIQRepository(Connection connection) {
        this.connection = connection;
        this.statements = new HashMap<>();
        
        // Initialize cache with 10 minute expiry
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, List<LoanIQEntity>>() {
                @Override
                public List<LoanIQEntity> load(String key) throws Exception {
                    return loadFromDatabase(key);
                }
            });
        
        try {
            prepareStatements();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to prepare statements", e);
        }
    }
    
    private void prepareStatements() throws SQLException {
        try {
            URI uri = getClass().getClassLoader().getResource("sql").toURI();
            try (Stream<Path> paths = Files.walk(Paths.get(uri))) {
                paths.filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                String query = new String(Files.readAllBytes(path));
                                String key = path.getFileName().toString().replace(".sql", "");
                                statements.put(key, connection.prepareStatement(query));
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to prepare statement for: " + path, e);
                            }
                        });
            }
        } catch (Exception e) {
            throw new SQLException("Error loading queries from sql directory", e);
        }
    }
    
    /**
     * Find entities by MEI
     */
    public List<LoanIQEntity> findByMEI(String mei) {
        if (mei == null) return new ArrayList<>();
        
        try {
            return cache.get("MEI:" + mei);
        } catch (Exception e) {
            logger.error("Error finding by MEI: {}", mei, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find entities by LEI
     */
    public List<LoanIQEntity> findByLEI(String lei) {
        if (lei == null) return new ArrayList<>();
        
        try {
            return cache.get("LEI:" + lei);
        } catch (Exception e) {
            logger.error("Error finding by LEI: {}", lei, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find entities by EIN
     */
    public List<LoanIQEntity> findByEIN(String ein) {
        if (ein == null) return new ArrayList<>();
        
        try {
            return cache.get("EIN:" + ein);
        } catch (Exception e) {
            logger.error("Error finding by EIN: {}", ein, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find entities by Debt Domain ID
     */
    public List<LoanIQEntity> findByDebtDomainId(String debtDomainId) {
        if (debtDomainId == null) return new ArrayList<>();
        
        return executeQuery("findByDebtDomainId", debtDomainId);
    }
    
    /**
     * Find candidates by name
     */
    public List<LoanIQEntity> findCandidatesByName(String legalName, String fundManager) {
        List<LoanIQEntity> candidates = new ArrayList<>();
        
        if (legalName != null) {
            String pattern = "%" + legalName.toLowerCase() + "%";
            String fmPattern = fundManager != null ? 
                "%" + fundManager.toLowerCase() + "%" : pattern;
            
            PreparedStatement stmt = statements.get("findByName");
            try {
                stmt.setString(1, pattern);
                stmt.setString(2, pattern);
                stmt.setString(3, fmPattern);
                stmt.setString(4, legalName);
                stmt.setString(5, legalName);
                
                candidates.addAll(executeStatement(stmt));
            } catch (SQLException e) {
                logger.error("Error finding candidates by name", e);
            }
        }
        
        return candidates;
    }
    
    /**
     * Find entities by email domain
     */
    public List<LoanIQEntity> findByEmailDomain(String emailDomain) {
        if (emailDomain == null) return new ArrayList<>();
        
        String domainPattern = "%" + emailDomain.split("\\.")[0] + "%";
        return executeQuery("findByEmailDomain", emailDomain, domainPattern, domainPattern);
    }
    
    /**
     * Find entities by cleaned short name (for duplicate detection)
     */
    public List<LoanIQEntity> findByCleanedShortName(String cleanedShortName) {
        if (cleanedShortName == null) return new ArrayList<>();
        
        return executeQuery("findByCleanedShortName",
            cleanedShortName.toLowerCase().replaceAll("[^a-z0-9]", ""));
    }
    
    /**
     * Find entity by ID
     */
    public LoanIQEntity findById(Long entityId) {
        if (entityId == null) return null;
        
        List<LoanIQEntity> results = executeQuery("findById", entityId);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Load data from database (called by cache)
     */
    private List<LoanIQEntity> loadFromDatabase(String cacheKey) throws Exception {
        String[] parts = cacheKey.split(":", 2);
        if (parts.length != 2) {
            return new ArrayList<>();
        }
        
        String type = parts[0];
        String value = parts[1];
        
        switch (type) {
            case "MEI":
                return executeQuery("findByMEI", value, value);
            case "LEI":
                return executeQuery("findByLEI", value, value);
            case "EIN":
                return executeQuery("findByEIN", value, value);
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Execute a query with parameters
     */
    private List<LoanIQEntity> executeQuery(String statementKey, Object... params) {
        PreparedStatement stmt = statements.get(statementKey);
        if (stmt == null) {
            logger.error("No prepared statement for key: {}", statementKey);
            return new ArrayList<>();
        }
        
        try {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return executeStatement(stmt);
        } catch (SQLException e) {
            logger.error("Query execution failed for: {}", statementKey, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Execute a prepared statement
     */
    private List<LoanIQEntity> executeStatement(PreparedStatement stmt) throws SQLException {
        List<LoanIQEntity> results = new ArrayList<>();
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs));
            }
        }
        
        logger.debug("Query returned {} results", results.size());
        return results;
    }
    
    /**
     * Map ResultSet to LoanIQEntity
     */
    private LoanIQEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        LoanIQEntity entity = new LoanIQEntity();
        
        entity.setEntityId(rs.getLong("entity_id"));
        entity.setFullName(rs.getString("full_name"));
        entity.setShortName(rs.getString("short_name"));
        entity.setUltimateParent(rs.getString("ultimate_parent"));
        entity.setMei(rs.getString("mei"));
        entity.setLei(rs.getString("lei"));
        entity.setEin(rs.getString("ein"));
        entity.setDebtDomainId(rs.getString("debt_domain_id"));
        entity.setCountryCode(rs.getString("country_code"));
        entity.setLegalAddress(rs.getString("legal_address"));
        entity.setTaxAddress(rs.getString("tax_address"));
        
        // Check if this is a location record
        try {
            String recordType = rs.getString("record_type");
            if ("LOCATION".equals(recordType)) {
                entity.setLocation(true);
                Long parentId = rs.getLong("parent_customer_id");
                if (!rs.wasNull()) {
                    entity.setParentCustomerId(parentId);
                }
            } else {
                entity.setLocation(false);
            }
        } catch (SQLException e) {
            // Column doesn't exist, assume main entity
            entity.setLocation(false);
        }
        
        // Get last modified if available
        try {
            Timestamp lastModified = rs.getTimestamp("last_modified");
            if (lastModified != null) {
                entity.setLastModified(lastModified.toLocalDateTime());
            }
        } catch (SQLException e) {
            // Column might not exist
        }
        
        return entity;
    }
    
    /**
     * Close resources
     */
    public void close() {
        try {
            for (PreparedStatement stmt : statements.values()) {
                stmt.close();
            }
            
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            
            logger.info("Repository closed successfully");
        } catch (SQLException e) {
            logger.error("Error closing repository", e);
        }
    }
    
    /**
     * Clear cache
     */
    public void clearCache() {
        cache.invalidateAll();
        logger.info("Cache cleared");
    }
    
    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        return cache.stats().toString();
    }
}