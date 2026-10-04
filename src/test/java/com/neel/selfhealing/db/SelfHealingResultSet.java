package com.neel.selfhealing.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Intelligent JDBC ResultSet wrapper that provides self-healing column lookup.
 * Automatically resolves renamed columns (e.g., customer_name -> customerName).
 */
public class SelfHealingResultSet implements AutoCloseable {

    private final ResultSet delegate;
    private final String tableName;
    private final String testName;
    private final DatabaseHealingEngine healingEngine;
    private final Set<String> actualColumnNames = new HashSet<>();

    public SelfHealingResultSet(ResultSet delegate, String tableName, String testName) throws SQLException {
        this.delegate = delegate;
        this.tableName = tableName;
        this.testName = testName != null ? testName : "DatabaseTest";
        this.healingEngine = DatabaseHealingEngine.getInstance();
        cacheColumnNames();
    }

    private void cacheColumnNames() throws SQLException {
        ResultSetMetaData md = delegate.getMetaData();
        int count = md.getColumnCount();
        for (int i = 1; i <= count; i++) {
            actualColumnNames.add(md.getColumnLabel(i).toUpperCase(Locale.ROOT));
            actualColumnNames.add(md.getColumnName(i).toUpperCase(Locale.ROOT));
        }
    }

    public boolean next() throws SQLException {
        return delegate.next();
    }

    public String getString(String columnLabel) throws SQLException {
        String resolvedLabel = resolveColumn(columnLabel, "STRING");
        return delegate.getString(resolvedLabel);
    }

    public int getInt(String columnLabel) throws SQLException {
        String resolvedLabel = resolveColumn(columnLabel, "INTEGER");
        return delegate.getInt(resolvedLabel);
    }

    public long getLong(String columnLabel) throws SQLException {
        String resolvedLabel = resolveColumn(columnLabel, "BIGINT");
        return delegate.getLong(resolvedLabel);
    }

    public boolean getBoolean(String columnLabel) throws SQLException {
        String resolvedLabel = resolveColumn(columnLabel, "BOOLEAN");
        return delegate.getBoolean(resolvedLabel);
    }

    public Object getObject(String columnLabel) throws SQLException {
        String resolvedLabel = resolveColumn(columnLabel, "OBJECT");
        return delegate.getObject(resolvedLabel);
    }

    private String resolveColumn(String columnLabel, String typeHint) throws SQLException {
        // Direct match check
        if (actualColumnNames.contains(columnLabel.toUpperCase(Locale.ROOT))) {
            return columnLabel;
        }

        // Heal column
        String healed = healingEngine.healColumn(testName, tableName, columnLabel, typeHint);
        if (actualColumnNames.contains(healed.toUpperCase(Locale.ROOT))) {
            return healed;
        }

        // Return original so JDBC throws column not found (safe failure)
        return columnLabel;
    }

    public ResultSet getDelegate() {
        return delegate;
    }

    public void close() throws SQLException {
        delegate.close();
    }
}
