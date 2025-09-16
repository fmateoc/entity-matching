package com.loantrading.matching.repository;

import com.loantrading.matching.entity.LoanIQEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseIntegrationTest {

    private Connection connection;
    private LoanIQRepository repository;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        createSchema();
        repository = new LoanIQRepository(connection);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS entity_locations");
            statement.execute("DROP TABLE IF EXISTS entities");
        }
        repository.close();
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
                    "last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            statement.execute("CREATE TABLE IF NOT EXISTS entity_locations (" +
                    "location_id BIGINT PRIMARY KEY, " +
                    "parent_customer_id BIGINT, " +
                    "mei VARCHAR(10), " +
                    "lei VARCHAR(20), " +
                    "ein VARCHAR(20), " +
                    "FOREIGN KEY (location_id) REFERENCES entities(entity_id), " +
                    "FOREIGN KEY (parent_customer_id) REFERENCES entities(entity_id))");
        }
    }

    private void insertTestData() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name, mei, lei, ein) VALUES " +
                    "(1, 'Test Corp', 'TestCo', 'MEI123', 'LEI456', 'EIN789')");
            stmt.execute("INSERT INTO entities (entity_id, full_name, short_name) VALUES " +
                    "(2, 'Location LLC', 'LocLLC')");
            stmt.execute("INSERT INTO entity_locations (location_id, parent_customer_id, mei, lei, ein) VALUES " +
                    "(2, 1, 'MEI123', 'LEI456', 'EIN789')");
        }
    }

    @Test
    public void testFindById() throws SQLException {
        insertTestData();
        LoanIQEntity entity = repository.findById(1L);
        assertNotNull(entity);
        assertEquals("Test Corp", entity.getFullName());
    }

    @Test
    public void testFindByMei() throws SQLException {
        insertTestData();
        List<LoanIQEntity> entities = repository.findByMEI("MEI123");
        assertEquals(2, entities.size());
    }

    @Test
    public void testFindByLei() throws SQLException {
        insertTestData();
        List<LoanIQEntity> entities = repository.findByLEI("LEI456");
        assertEquals(2, entities.size());
    }

    @Test
    public void testFindByEin() throws SQLException {
        insertTestData();
        List<LoanIQEntity> entities = repository.findByEIN("EIN789");
        assertEquals(2, entities.size());
    }
}
