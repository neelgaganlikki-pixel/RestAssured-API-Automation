package com.neel.selfhealing.report;

/**
 * Standardized failure classification for API Automation and Database Testing.
 * Complies with Section 31 of architectural specifications.
 */
public enum FailureCategory {

    // API Failure Classifications
    API_LOCATOR_MAPPING_FAILURE("API LOCATOR/MAPPING FAILURE", "API"),
    API_CONTRACT_FAILURE("API CONTRACT FAILURE", "API"),
    API_APPLICATION_FAILURE("API APPLICATION FAILURE", "API"),
    AUTHENTICATION_FAILURE("AUTHENTICATION FAILURE", "API"),
    DATA_FAILURE("DATA FAILURE", "COMMON"),
    ENVIRONMENT_FAILURE("ENVIRONMENT FAILURE", "COMMON"),
    ASSERTION_FAILURE("ASSERTION FAILURE", "COMMON"),

    // Database Failure Classifications
    SCHEMA_MAPPING_FAILURE("SCHEMA/MAPPING FAILURE", "DATABASE"),
    DATABASE_CONNECTION_FAILURE("DATABASE CONNECTION FAILURE", "DATABASE"),
    QUERY_FAILURE("QUERY FAILURE", "DATABASE"),
    DATABASE_APPLICATION_FAILURE("DATABASE APPLICATION FAILURE", "DATABASE");

    private final String displayName;
    private final String domain;

    FailureCategory(String displayName, String domain) {
        this.displayName = displayName;
        this.domain = domain;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

