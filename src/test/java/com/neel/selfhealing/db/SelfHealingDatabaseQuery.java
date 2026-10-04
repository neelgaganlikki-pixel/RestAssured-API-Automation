package com.neel.selfhealing.db;

import com.neel.selfhealing.report.FailureClassifier;
import com.neel.selfhealing.report.SelfHealingEvent;
import com.neel.selfhealing.report.SelfHealingReporter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes database queries with self-healing capabilities.
 * Strictly verifies READ-ONLY safety.
 * Recovers from renamed tables or columns without mutating the original SQL files.
 */
public class SelfHealingDatabaseQuery {

    private final Connection connection;
    private final String testName;
    private final DatabaseHealingEngine healingEngine;

    public SelfHealingDatabaseQuery(Connection connection, String testName) {
        this.connection = connection;
        this.testName = testName != null ? testName : "DatabaseTest";
        this.healingEngine = DatabaseHealingEngine.getInstance();
    }

    public SelfHealingResultSet executeQuery(String sql) throws SQLException {
        // Enforce Read-Only safety
        DatabaseSafetyGuard.validateReadOnly(sql);

        // Ensure schema is discovered
        healingEngine.getSchemaDiscovery().discoverSchema(connection, null, null);

        String tableName = extractTableName(sql);
        Statement stmt = connection.createStatement();

        try {
            // 1. Attempt original query first
            ResultSet rs = stmt.executeQuery(sql);
            return new SelfHealingResultSet(rs, tableName, testName);
        } catch (SQLException originalEx) {
            // 2. Query failed (e.g. column or table missing)
            String healedSql = healingEngine.healQuery(testName, sql);

            if (!healedSql.equalsIgnoreCase(sql)) {
                try {
                    ResultSet rs = stmt.executeQuery(healedSql);
                    return new SelfHealingResultSet(rs, extractTableName(healedSql), testName);
                } catch (SQLException healedEx) {
                    SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                            "DATABASE",
                            testName,
                            "QUERY",
                            sql,
                            healedSql,
                            0.0,
                            null,
                            SelfHealingEvent.Status.FAILED,
                            "FAILED",
                            FailureClassifier.classifyDbFailure(healedEx, healedSql),
                            "Healed query execution failed: " + healedEx.getMessage()
                    ));
                    throw healedEx;
                }
            }

            // Could not heal query, classify failure and throw original exception
            SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                    "DATABASE",
                    testName,
                    "QUERY",
                    sql,
                    "NONE",
                    0.0,
                    null,
                    SelfHealingEvent.Status.REJECTED,
                    "FAILED",
                    FailureClassifier.classifyDbFailure(originalEx, sql),
                    originalEx.getMessage()
            ));

            throw originalEx;
        }
    }

    private String extractTableName(String sql) {
        Pattern pattern = Pattern.compile("(?i)\\bFROM\\s+([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "UNKNOWN_TABLE";
    }
}

