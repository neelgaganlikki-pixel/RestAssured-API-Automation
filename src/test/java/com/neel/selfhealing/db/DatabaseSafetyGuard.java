package com.neel.selfhealing.db;

import java.util.regex.Pattern;

/**
 * Enforces Database Safety: ensures self-healing operations are strictly READ-ONLY.
 * Prevents automated execution of destructive SQL (DROP, DELETE, TRUNCATE, ALTER, UPDATE, INSERT).
 */
public final class DatabaseSafetyGuard {

    private static final Pattern DESTRUCTIVE_SQL_PATTERN = Pattern.compile(
            "(?i)^\\s*(DROP|DELETE|TRUNCATE|ALTER|UPDATE|INSERT|GRANT|REVOKE|MERGE)\\b"
    );

    private DatabaseSafetyGuard() {
    }

    public static void validateReadOnly(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }

        String trimmed = sql.trim();
        if (DESTRUCTIVE_SQL_PATTERN.matcher(trimmed).find()) {
            throw new SecurityException("[DB-SAFETY-VIOLATION] Self-healing engine is strictly READ-ONLY. " +
                    "Execution of destructive SQL is disallowed: " + trimmed);
        }
    }

    public static boolean isReadOnly(String sql) {
        if (sql == null) return true;
        return !DESTRUCTIVE_SQL_PATTERN.matcher(sql.trim()).find();
    }
}

