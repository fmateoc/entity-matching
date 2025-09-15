package com.loantrading.matching.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseIntegrationTest {

    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        // Use H2 in-memory database with a unique name for each test run
        // MODE=PostgreSQL is used for compatibility
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        createSchema();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private void createSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS entities (" +
                    "entity_id BIGSERIAL PRIMARY KEY, " +
                    "full_name VARCHAR(500) NOT NULL, " +
                    "short_name VARCHAR(200), " +
                    "ultimate_parent VARCHAR(500), " +
                    "mei VARCHAR(10), " +
                    "lei VARCHAR(20), " +
                    "ein VARCHAR(20), " +
                    "debt_domain_id VARCHAR(20), " +
                    "email_domain VARCHAR(100), " +
                    "country_code VARCHAR(2), " +
                    "legal_address TEXT, " +
                    "tax_address TEXT, " +
                    "last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE (short_name)" +
                    ")");
        }
    }

    @Test
    public void shouldInsertAndRetrieveEntity() throws SQLException {
        // Insert a sample entity
        String fullName = "Test Entity Inc.";
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO entities (full_name) VALUES (?)")) {
            ps.setString(1, fullName);
            int rowsAffected = ps.executeUpdate();
            assertEquals(1, rowsAffected);
        }

        // Retrieve the entity and verify
        try (PreparedStatement ps = connection.prepareStatement("SELECT full_name FROM entities WHERE full_name = ?")) {
            ps.setString(1, fullName);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "Should find the inserted entity");
            assertEquals(fullName, rs.getString("full_name"));
        }
    }
}
