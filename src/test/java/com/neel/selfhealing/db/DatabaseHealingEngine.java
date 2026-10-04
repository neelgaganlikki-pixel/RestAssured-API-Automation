package com.neel.selfhealing.db;

import com.neel.selfhealing.confidence.ConfidenceEngine;
import com.neel.selfhealing.confidence.ConfidenceLevel;
import com.neel.selfhealing.confidence.ConfidenceScore;
import com.neel.selfhealing.config.SelfHealingConfig;
import com.neel.selfhealing.history.HealingHistoryManager;
import com.neel.selfhealing.history.HealingRecord;
import com.neel.selfhealing.report.FailureCategory;
import com.neel.selfhealing.report.SelfHealingEvent;
import com.neel.selfhealing.report.SelfHealingReporter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production Database Self-Healing Engine.
 * Recovers from renamed columns, renamed tables, and query syntax shifts.
 * Preserves strict assertion logic: never heals unexpected data values.
 * Strictly READ-ONLY.
 */
public class DatabaseHealingEngine {

    private static volatile DatabaseHealingEngine instance;

    private final ConfidenceEngine confidenceEngine = new ConfidenceEngine();
    private final DatabaseSchemaDiscovery schemaDiscovery = new DatabaseSchemaDiscovery();
    private final HealingHistoryManager historyManager = HealingHistoryManager.getInstance();

    private DatabaseHealingEngine() {
    }

    public static DatabaseHealingEngine getInstance() {
        if (instance == null) {
            synchronized (DatabaseHealingEngine.class) {
                if (instance == null) {
                    instance = new DatabaseHealingEngine();
                }
            }
        }
        return instance;
    }

    public DatabaseSchemaDiscovery getSchemaDiscovery() {
        return schemaDiscovery;
    }

    public ConfidenceEngine getConfidenceEngine() {
        return confidenceEngine;
    }

    /**
     * Heals a column name within a given table.
     */
    public String healColumn(String testName, String tableName, String expectedColumn, String expectedType) {
        if (expectedColumn == null || !SelfHealingConfig.getInstance().isDbSelfHealingEnabled()) {
            return expectedColumn;
        }

        // 1. Check if column exists as-is
        DatabaseMetadataModel.TableMetadata table = schemaDiscovery.getTable(tableName);
        if (table != null && table.getColumn(expectedColumn) != null) {
            return expectedColumn; // Exact match, no healing needed
        }

        // 2. Check persistent history
        String compositeKey = (tableName != null ? tableName + "." : "") + expectedColumn;
        HealingRecord cached = historyManager.lookup("DATABASE", compositeKey);
        if (cached != null && cached.getConfidence() >= SelfHealingConfig.getInstance().getConfidenceThreshold()) {
            if (table != null && table.getColumn(cached.getHealed()) != null) {
                historyManager.recordSuccessfulHealing(
                        "DATABASE",
                        compositeKey,
                        cached.getHealed(),
                        cached.getConfidence(),
                        testName
                );
                SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                        "DATABASE",
                        testName,
                        "COLUMN",
                        expectedColumn,
                        cached.getHealed(),
                        cached.getConfidence(),
                        ConfidenceLevel.fromScore(cached.getConfidence()),
                        SelfHealingEvent.Status.HEALED,
                        "PASSED",
                        null,
                        "Column healed from history: " + expectedColumn + " -> " + cached.getHealed()
                ));
                return cached.getHealed();
            }
        }

        // 3. Search candidate columns in table
        if (table != null) {
            String bestCandidate = null;
            DatabaseMetadataModel.ColumnMetadata bestCandidateMeta = null;
            ConfidenceScore bestScore = null;

            Map<String, Object> context = new HashMap<>();
            context.put("tableName", tableName);

            for (DatabaseMetadataModel.ColumnMetadata col : table.getColumns().values()) {
                String candidateName = col.getColumnName();
                String candidateType = col.getDataTypeName();

                ConfidenceScore score = confidenceEngine.evaluate(
                        expectedColumn,
                        candidateName,
                        expectedType != null ? expectedType : "UNKNOWN",
                        candidateType,
                        context,
                        0
                );

                if (score.getOverallScore() >= SelfHealingConfig.getInstance().getConfidenceThreshold()) {
                    if (bestScore == null || score.getOverallScore() > bestScore.getOverallScore()) {
                        bestScore = score;
                        bestCandidate = candidateName;
                        bestCandidateMeta = col;
                    }
                }
            }

            if (bestCandidate != null && bestScore != null) {
                int confidencePercent = bestScore.getPercentage();
                String dataType = bestCandidateMeta.getDataTypeName();

                // Log according to prompt Section 22
                SelfHealingReporter.getInstance().logDbHealing(
                        testName,
                        expectedColumn,
                        bestCandidate,
                        dataType,
                        confidencePercent,
                        "PASSED",
                        "SUCCESS"
                );

                // Record event
                SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                        "DATABASE",
                        testName,
                        "COLUMN",
                        expectedColumn,
                        bestCandidate,
                        bestScore.getOverallScore(),
                        bestScore.getLevel(),
                        SelfHealingEvent.Status.HEALED,
                        "PASSED",
                        null,
                        "Database column healed in table " + tableName + ": " + expectedColumn + " -> " + bestCandidate
                ));

                // Record history
                historyManager.recordSuccessfulHealing(
                        "DATABASE",
                        compositeKey,
                        bestCandidate,
                        bestScore.getOverallScore(),
                        testName
                );

                return bestCandidate;
            }
        }

        // Rejected/Failed healing
        SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                "DATABASE",
                testName,
                "COLUMN",
                expectedColumn,
                "NONE",
                0.0,
                ConfidenceLevel.LOW,
                SelfHealingEvent.Status.REJECTED,
                "FAILED",
                FailureCategory.SCHEMA_MAPPING_FAILURE,
                "No candidate column in table " + tableName + " met threshold"
        ));

        return expectedColumn;
    }

    /**
     * Heals a table name if renamed (e.g. "customers" -> "customer").
     */
    public String healTable(String testName, String expectedTable) {
        if (expectedTable == null || !SelfHealingConfig.getInstance().isDbSelfHealingEnabled()) {
            return expectedTable;
        }

        // Direct match check
        if (schemaDiscovery.getTable(expectedTable) != null) {
            return expectedTable;
        }

        // History lookup
        HealingRecord cached = historyManager.lookup("DATABASE", expectedTable);
        if (cached != null && cached.getConfidence() >= SelfHealingConfig.getInstance().getConfidenceThreshold()) {
            if (schemaDiscovery.getTable(cached.getHealed()) != null) {
                historyManager.recordSuccessfulHealing("DATABASE", expectedTable, cached.getHealed(), cached.getConfidence(), testName);
                SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                        "DATABASE",
                        testName,
                        "TABLE",
                        expectedTable,
                        cached.getHealed(),
                        cached.getConfidence(),
                        ConfidenceLevel.fromScore(cached.getConfidence()),
                        SelfHealingEvent.Status.HEALED,
                        "PASSED",
                        null,
                        "Table healed from history: " + expectedTable + " -> " + cached.getHealed()
                ));
                return cached.getHealed();
            }
        }

        String bestTable = null;
        ConfidenceScore bestScore = null;

        for (DatabaseMetadataModel.TableMetadata table : schemaDiscovery.getAllTables().values()) {
            String candidateTable = table.getTableName();
            ConfidenceScore score = confidenceEngine.evaluate(
                    expectedTable,
                    candidateTable,
                    "TABLE",
                    "TABLE",
                    Collections.emptyMap(),
                    0
            );

            if (score.getOverallScore() >= SelfHealingConfig.getInstance().getConfidenceThreshold()) {
                if (bestScore == null || score.getOverallScore() > bestScore.getOverallScore()) {
                    bestScore = score;
                    bestTable = candidateTable;
                }
            }
        }

        if (bestTable != null && bestScore != null) {
            SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                    "DATABASE",
                    testName,
                    "TABLE",
                    expectedTable,
                    bestTable,
                    bestScore.getOverallScore(),
                    bestScore.getLevel(),
                    SelfHealingEvent.Status.HEALED,
                    "PASSED",
                    null,
                    "Database table healed: " + expectedTable + " -> " + bestTable
            ));

            historyManager.recordSuccessfulHealing("DATABASE", expectedTable, bestTable, bestScore.getOverallScore(), testName);
            return bestTable;
        }

        // Failed/Rejected
        SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                "DATABASE",
                testName,
                "TABLE",
                expectedTable,
                "NONE",
                0.0,
                ConfidenceLevel.LOW,
                SelfHealingEvent.Status.REJECTED,
                "FAILED",
                FailureCategory.SCHEMA_MAPPING_FAILURE,
                "No candidate table met threshold for " + expectedTable
        ));

        return expectedTable;
    }

    /**
     * Query Healing: Heals SQL SELECT query by resolving renamed table and columns.
     */
    public String healQuery(String testName, String originalSql) {
        if (originalSql == null || !SelfHealingConfig.getInstance().isDbSelfHealingEnabled()) {
            return originalSql;
        }

        // Validate READ-ONLY safety first!
        DatabaseSafetyGuard.validateReadOnly(originalSql);

        String healedSql = originalSql;

        // 1. Identify FROM table
        Pattern fromPattern = Pattern.compile("(?i)\\bFROM\\s+([a-zA-Z0-9_]+)");
        Matcher fromMatcher = fromPattern.matcher(originalSql);
        String currentTable = null;
        if (fromMatcher.find()) {
            String origTable = fromMatcher.group(1);
            String healedTable = healTable(testName, origTable);
            if (!origTable.equalsIgnoreCase(healedTable)) {
                healedSql = healedSql.replaceAll("(?i)\\bFROM\\s+" + Pattern.quote(origTable) + "\\b", "FROM " + healedTable);
            }
            currentTable = healedTable;
        }

        // 2. Identify SELECT columns
        Pattern selectPattern = Pattern.compile("(?i)^\\s*SELECT\\s+(.+?)\\s+FROM\\b");
        Matcher selectMatcher = selectPattern.matcher(healedSql);
        if (selectMatcher.find() && currentTable != null) {
            String colsClause = selectMatcher.group(1).trim();
            if (!colsClause.equals("*")) {
                String[] cols = colsClause.split(",");
                StringBuilder newColsClause = new StringBuilder();
                boolean anyColHealed = false;

                for (int i = 0; i < cols.length; i++) {
                    String col = cols[i].trim();
                    String healedCol = healColumn(testName, currentTable, col, null);
                    if (!col.equalsIgnoreCase(healedCol)) {
                        anyColHealed = true;
                    }
                    if (i > 0) newColsClause.append(", ");
                    newColsClause.append(healedCol);
                }

                if (anyColHealed) {
                    healedSql = selectMatcher.replaceFirst("SELECT " + newColsClause.toString() + " FROM");
                }
            }
        }

        if (!healedSql.equalsIgnoreCase(originalSql)) {
            SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                    "DATABASE",
                    testName,
                    "QUERY",
                    originalSql,
                    healedSql,
                    0.95,
                    ConfidenceLevel.HIGH,
                    SelfHealingEvent.Status.HEALED,
                    "PASSED",
                    null,
                    "Query healed safely: " + originalSql + " -> " + healedSql
            ));
        }

        return healedSql;
    }
}

