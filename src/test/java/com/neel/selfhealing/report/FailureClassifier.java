package com.neel.selfhealing.report;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLException;

/**
 * Intelligent classifier that categorizes failures into distinct architectural categories
 * ensuring genuine defects are distinguished from recoverable mapping changes.
 */
public class FailureClassifier {

    public static FailureCategory classifyApiFailure(Throwable error, Integer statusCode, String responseBody) {
        if (statusCode != null) {
            if (statusCode == 401 || statusCode == 403) {
                return FailureCategory.AUTHENTICATION_FAILURE;
            }
            if (statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504) {
                return FailureCategory.API_APPLICATION_FAILURE;
            }
            if (statusCode == 404) {
                return FailureCategory.API_LOCATOR_MAPPING_FAILURE;
            }
            if (statusCode == 400 || statusCode == 422) {
                return FailureCategory.API_CONTRACT_FAILURE;
            }
        }

        if (error != null) {
            if (error instanceof AssertionError) {
                String msg = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
                if (msg.contains("expected") && (msg.contains("found") || msg.contains("but was"))) {
                    if (msg.contains("status") || msg.contains("schema") || msg.contains("type")) {
                        return FailureCategory.API_CONTRACT_FAILURE;
                    }
                    return FailureCategory.ASSERTION_FAILURE;
                }
                return FailureCategory.ASSERTION_FAILURE;
            }

            if (error instanceof ConnectException || error instanceof UnknownHostException || error instanceof SocketTimeoutException) {
                return FailureCategory.ENVIRONMENT_FAILURE;
            }

            String msg = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
            if (msg.contains("jsonpath") || msg.contains("path") || msg.contains("cannot find") || msg.contains("nullpointer")) {
                return FailureCategory.API_LOCATOR_MAPPING_FAILURE;
            }
            if (msg.contains("schema") || msg.contains("contract")) {
                return FailureCategory.API_CONTRACT_FAILURE;
            }
        }

        return FailureCategory.API_APPLICATION_FAILURE;
    }

    public static FailureCategory classifyDbFailure(Throwable error, String sql) {
        if (error != null) {
            if (error instanceof AssertionError) {
                return FailureCategory.ASSERTION_FAILURE;
            }

            if (error instanceof SQLException) {
                SQLException sqlEx = (SQLException) error;
                String state = sqlEx.getSQLState() != null ? sqlEx.getSQLState() : "";
                String msg = sqlEx.getMessage() != null ? sqlEx.getMessage().toLowerCase() : "";

                // Connection error codes (08001, 08003, etc.)
                if (state.startsWith("08") || msg.contains("connection") || msg.contains("refused") || msg.contains("timeout")) {
                    return FailureCategory.DATABASE_CONNECTION_FAILURE;
                }

                // Table or Column missing (42S02, 42S22, 42000, etc.)
                if (state.startsWith("42") || msg.contains("not found") || msg.contains("column") || msg.contains("table") || msg.contains("unknown column")) {
                    return FailureCategory.SCHEMA_MAPPING_FAILURE;
                }

                // Syntax error or constraint failure
                if (state.startsWith("23")) {
                    return FailureCategory.DATA_FAILURE;
                }

                return FailureCategory.QUERY_FAILURE;
            }

            if (error instanceof ConnectException || error instanceof SocketTimeoutException) {
                return FailureCategory.ENVIRONMENT_FAILURE;
            }

            String msg = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
            if (msg.contains("column") || msg.contains("table") || msg.contains("schema")) {
                return FailureCategory.SCHEMA_MAPPING_FAILURE;
            }
            if (msg.contains("data") || msg.contains("mismatch")) {
                return FailureCategory.DATA_FAILURE;
            }
        }

        return FailureCategory.DATABASE_APPLICATION_FAILURE;
    }
}

