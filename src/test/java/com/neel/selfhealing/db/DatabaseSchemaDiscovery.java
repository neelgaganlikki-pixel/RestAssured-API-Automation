package com.neel.selfhealing.db;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers and caches database schema metadata dynamically using JDBC DatabaseMetaData.
 * Reads tables, columns, data types, nullability, primary keys, and foreign keys.
 * Operates strictly with read metadata; never leaks credentials.
 */
public class DatabaseSchemaDiscovery {

    private final Map<String, DatabaseMetadataModel.TableMetadata> tableMetadataCache = new ConcurrentHashMap<>();

    public DatabaseSchemaDiscovery() {
    }

    public synchronized void discoverSchema(Connection connection, String catalog, String schemaPattern) {
        if (connection == null) return;

        try {
            DatabaseMetaData metaData = connection.getMetaData();

            // 1. Discover Tables and Views
            try (ResultSet rsTables = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE", "VIEW"})) {
                while (rsTables.next()) {
                    String tableName = rsTables.getString("TABLE_NAME");
                    if (tableName != null) {
                        DatabaseMetadataModel.TableMetadata tableMeta = new DatabaseMetadataModel.TableMetadata(tableName);
                        tableMetadataCache.put(tableName.toUpperCase(Locale.ROOT), tableMeta);
                    }
                }
            }

            // 2. Discover Columns for each table
            for (DatabaseMetadataModel.TableMetadata tableMeta : tableMetadataCache.values()) {
                String tableName = tableMeta.getTableName();
                try (ResultSet rsCols = metaData.getColumns(catalog, schemaPattern, tableName, "%")) {
                    while (rsCols.next()) {
                        String colName = rsCols.getString("COLUMN_NAME");
                        int dataType = rsCols.getInt("DATA_TYPE");
                        String typeName = rsCols.getString("TYPE_NAME");
                        int nullableVal = rsCols.getInt("NULLABLE");
                        boolean nullable = (nullableVal == DatabaseMetaData.columnNullable);

                        DatabaseMetadataModel.ColumnMetadata colMeta = new DatabaseMetadataModel.ColumnMetadata(
                                colName,
                                dataType,
                                typeName,
                                nullable
                        );
                        tableMeta.addColumn(colMeta);
                    }
                }

                // 3. Discover Primary Keys
                try (ResultSet rsPK = metaData.getPrimaryKeys(catalog, schemaPattern, tableName)) {
                    while (rsPK.next()) {
                        String pkCol = rsPK.getString("COLUMN_NAME");
                        if (pkCol != null) {
                            tableMeta.addPrimaryKey(pkCol);
                        }
                    }
                } catch (Exception ignored) {
                }

                // 4. Discover Foreign Keys (Imported Keys)
                try (ResultSet rsFK = metaData.getImportedKeys(catalog, schemaPattern, tableName)) {
                    while (rsFK.next()) {
                        String fkCol = rsFK.getString("FKCOLUMN_NAME");
                        String pkTable = rsFK.getString("PKTABLE_NAME");
                        String pkCol = rsFK.getString("PKCOLUMN_NAME");
                        if (fkCol != null && pkTable != null && pkCol != null) {
                            tableMeta.addForeignKey(fkCol, pkTable, pkCol);
                        }
                    }
                } catch (Exception ignored) {
                }
            }

        } catch (SQLException e) {
            System.err.println("[DB-SELF-HEALING] Warning: Error during schema discovery: " + e.getMessage());
        }
    }

    public DatabaseMetadataModel.TableMetadata getTable(String tableName) {
        if (tableName == null) return null;
        return tableMetadataCache.get(tableName.toUpperCase(Locale.ROOT));
    }

    public Map<String, DatabaseMetadataModel.TableMetadata> getAllTables() {
        return Collections.unmodifiableMap(tableMetadataCache);
    }

    public void clearCache() {
        tableMetadataCache.clear();
    }
}

