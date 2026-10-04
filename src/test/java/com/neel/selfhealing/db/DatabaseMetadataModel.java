package com.neel.selfhealing.db;

import java.util.*;

/**
 * Metadata model capturing database schema structure, columns, types, and relationships.
 */
public class DatabaseMetadataModel {

    public static class ColumnMetadata {
        private final String columnName;
        private final int dataType;
        private final String dataTypeName;
        private final boolean nullable;
        private boolean isPrimaryKey;
        private boolean isForeignKey;

        public ColumnMetadata(String columnName, int dataType, String dataTypeName, boolean nullable) {
            this.columnName = columnName;
            this.dataType = dataType;
            this.dataTypeName = dataTypeName;
            this.nullable = nullable;
        }

        public String getColumnName() {
            return columnName;
        }

        public int getDataType() {
            return dataType;
        }

        public String getDataTypeName() {
            return dataTypeName;
        }

        public boolean isNullable() {
            return nullable;
        }

        public boolean isPrimaryKey() {
            return isPrimaryKey;
        }

        public void setPrimaryKey(boolean primaryKey) {
            isPrimaryKey = primaryKey;
        }

        public boolean isForeignKey() {
            return isForeignKey;
        }

        public void setForeignKey(boolean foreignKey) {
            isForeignKey = foreignKey;
        }
    }

    public static class TableMetadata {
        private final String tableName;
        private final Map<String, ColumnMetadata> columns = new LinkedHashMap<>();
        private final Set<String> primaryKeys = new LinkedHashSet<>();
        private final Map<String, String> foreignKeyRelationships = new LinkedHashMap<>(); // fkCol -> pkTable.pkCol

        public TableMetadata(String tableName) {
            this.tableName = tableName;
        }

        public String getTableName() {
            return tableName;
        }

        public void addColumn(ColumnMetadata column) {
            columns.put(column.getColumnName().toUpperCase(Locale.ROOT), column);
        }

        public ColumnMetadata getColumn(String columnName) {
            if (columnName == null) return null;
            return columns.get(columnName.toUpperCase(Locale.ROOT));
        }

        public Map<String, ColumnMetadata> getColumns() {
            return Collections.unmodifiableMap(columns);
        }

        public void addPrimaryKey(String columnName) {
            primaryKeys.add(columnName.toUpperCase(Locale.ROOT));
            ColumnMetadata col = getColumn(columnName);
            if (col != null) col.setPrimaryKey(true);
        }

        public Set<String> getPrimaryKeys() {
            return Collections.unmodifiableSet(primaryKeys);
        }

        public void addForeignKey(String fkColumn, String pkTable, String pkColumn) {
            foreignKeyRelationships.put(fkColumn.toUpperCase(Locale.ROOT), pkTable + "." + pkColumn);
            ColumnMetadata col = getColumn(fkColumn);
            if (col != null) col.setForeignKey(true);
        }

        public Map<String, String> getForeignKeyRelationships() {
            return Collections.unmodifiableMap(foreignKeyRelationships);
        }
    }
}

