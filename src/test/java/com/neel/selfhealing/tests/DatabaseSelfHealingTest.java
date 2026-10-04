package com.neel.selfhealing.tests;

import com.neel.selfhealing.confidence.ConfidenceEngine;
import com.neel.selfhealing.confidence.ConfidenceScore;
import com.neel.selfhealing.config.SelfHealingConfig;
import com.neel.selfhealing.db.DatabaseHealingEngine;
import com.neel.selfhealing.db.SelfHealingDatabaseQuery;
import com.neel.selfhealing.db.SelfHealingResultSet;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collections;

import static org.testng.Assert.*;

/**
 * Verification Suite for Database Self-Healing Engine.
 * Implements the 10 mandatory Database verification tests specified in Section 29.
 */
public class DatabaseSelfHealingTest {

    private Connection connection;
    private DatabaseHealingEngine dbHealingEngine;

    @BeforeClass
    public void setupDatabase() throws Exception {
        // In-memory H2 database setup
        connection = DriverManager.getConnection("jdbc:h2:mem:selftest;DB_CLOSE_DELAY=-1", "sa", "");
        dbHealingEngine = DatabaseHealingEngine.getInstance();

        try (Statement stmt = connection.createStatement()) {
            // Create "customer" table (renamed from customers, and customerName instead of customer_name)
            stmt.execute("CREATE TABLE customer (" +
                    "id INT PRIMARY KEY, " +
                    "customerName VARCHAR(100), " +
                    "email VARCHAR(100)" +
                    ");");
            stmt.execute("INSERT INTO customer (id, customerName, email) VALUES (1, 'Neel', 'neel@example.com');");

            // Create "orders" table with relationships
            stmt.execute("CREATE TABLE orders (" +
                    "order_id INT PRIMARY KEY, " +
                    "customer_id INT, " +
                    "product_id INT, " +
                    "orderDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "order_status VARCHAR(50)" +
                    ");");
            stmt.execute("INSERT INTO orders (order_id, customer_id, product_id, order_status) VALUES (101, 1, 999, 'DELIVERED');");
        }

        // Trigger schema discovery
        dbHealingEngine.getSchemaDiscovery().discoverSchema(connection, null, null);
    }

    @AfterClass
    public void tearDownDatabase() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // 1. Original column exists
    @Test
    public void test1_OriginalColumnExists() {
        String healed = dbHealingEngine.healColumn("test1_OriginalColumnExists", "customer", "email", "VARCHAR");
        assertEquals(healed, "email", "Existing column should match directly without healing");
    }

    // 2. Column renamed (customer_name -> customerName)
    @Test
    public void test2_ColumnRenamed() {
        String healed = dbHealingEngine.healColumn("test2_ColumnRenamed", "customer", "customer_name", "VARCHAR");
        assertTrue(healed.equalsIgnoreCase("customerName"), "Column 'customer_name' should heal to 'customerName'");
    }

    // 3. Table renamed (customers -> customer)
    @Test
    public void test3_TableRenamed() {
        String healed = dbHealingEngine.healTable("test3_TableRenamed", "customers");
        assertTrue(healed.equalsIgnoreCase("customer"), "Table 'customers' should heal to 'customer'");
    }

    // 4. Data type changed (evaluates type compatibility)
    @Test
    public void test4_DataTypeChanged() {
        ConfidenceEngine ce = dbHealingEngine.getConfidenceEngine();

        // Compatible numeric types
        double scoreIntToInt = ce.evaluateTypeCompatibility("INTEGER", "BIGINT");
        assertTrue(scoreIntToInt >= 0.90, "Compatible numeric types should have high score");

        // Incompatible types: Integer to String
        double scoreIntToStr = ce.evaluateTypeCompatibility("INTEGER", "VARCHAR");
        assertTrue(scoreIntToStr <= 0.20, "Incompatible types must have low score and not heal");
    }

    // 5. Multiple possible columns
    @Test
    public void test5_MultiplePossibleColumns() {
        String healed = dbHealingEngine.healColumn("test5_MultiplePossibleColumns", "customer", "name", "VARCHAR");
        assertTrue(healed.equalsIgnoreCase("customerName"), "Should disambiguate and heal 'name' to 'customerName'");
    }

    // 6. High-confidence mapping
    @Test
    public void test6_HighConfidenceMapping() {
        String healed = dbHealingEngine.healColumn("test6_HighConfidenceMapping", "orders", "order_date", "TIMESTAMP");
        assertTrue(healed.equalsIgnoreCase("orderDate"), "order_date should heal to orderDate with high confidence");
    }

    // 7. Low-confidence mapping
    @Test
    public void test7_LowConfidenceMapping() {
        ConfidenceScore score = dbHealingEngine.getConfidenceEngine().evaluate(
                "shipping_address",
                "product_id",
                "VARCHAR",
                "INTEGER",
                Collections.emptyMap(),
                0
        );

        assertTrue(score.getOverallScore() < SelfHealingConfig.getInstance().getConfidenceThreshold(),
                "Unrelated field mapping should fall below confidence threshold");
    }

    // 8. Relationship validation (customer_id must NOT map to product_id)
    @Test
    public void test8_RelationshipValidation() {
        ConfidenceScore score = dbHealingEngine.getConfidenceEngine().evaluate(
                "customer_id",
                "product_id",
                "INTEGER",
                "INTEGER",
                Collections.emptyMap(),
                0
        );

        assertTrue(score.getOverallScore() < 0.50,
                "Cross-entity relation mapping (customer_id -> product_id) must be rejected");
    }

    // 9. Query healing (heals SELECT customer_name FROM customers)
    @Test
    public void test9_QueryHealing() throws Exception {
        SelfHealingDatabaseQuery queryExec = new SelfHealingDatabaseQuery(connection, "test9_QueryHealing");

        // Query with renamed table "customers" and renamed column "customer_name"
        String sql = "SELECT customer_name FROM customers WHERE id = 1";
        try (SelfHealingResultSet rs = queryExec.executeQuery(sql)) {
            assertTrue(rs.next(), "Query should return at least one row");
            String name = rs.getString("customer_name");
            assertEquals(name, "Neel", "ResultSet should read 'customer_name' through healed column 'customerName'");
        }
    }

    // 10. Healing disabled
    @Test
    public void test10_HealingDisabled() {
        try {
            System.setProperty("DB_SELF_HEALING_ENABLED", "false");
            SelfHealingConfig.reload();

            String healed = dbHealingEngine.healColumn("test10_HealingDisabled", "customer", "customer_name", "VARCHAR");
            assertEquals(healed, "customer_name", "When disabled, healing should not take place");
        } finally {
            System.clearProperty("DB_SELF_HEALING_ENABLED");
            SelfHealingConfig.reload();
        }
    }
}
