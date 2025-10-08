-- H2 Database Schema for Integration Tests

CREATE TABLE IF NOT EXISTS entities (
    entity_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(500) NOT NULL,
    short_name VARCHAR(200),
    ultimate_parent VARCHAR(500),
    mei VARCHAR(10),
    lei VARCHAR(20),
    ein VARCHAR(20),
    debt_domain_id VARCHAR(20),
    email_domain VARCHAR(100),
    country_code VARCHAR(2),
    legal_address TEXT,
    tax_address TEXT,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS entity_locations (
    location_id BIGINT PRIMARY KEY,
    parent_customer_id BIGINT,
    mei VARCHAR(10),
    lei VARCHAR(20),
    ein VARCHAR(20),
    FOREIGN KEY (location_id) REFERENCES entities(entity_id),
    FOREIGN KEY (parent_customer_id) REFERENCES entities(entity_id)
);