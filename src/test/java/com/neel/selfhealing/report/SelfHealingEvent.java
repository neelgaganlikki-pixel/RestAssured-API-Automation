package com.neel.selfhealing.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.neel.selfhealing.confidence.ConfidenceLevel;

import java.time.Instant;

/**
 * Event recording a single healing attempt (success, rejected, or failed).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelfHealingEvent {

    public enum Status {
        HEALED,
        REJECTED,
        FAILED
    }

    @JsonProperty("domain")
    private String domain; // "API" or "DATABASE"

    @JsonProperty("test_name")
    private String testName;

    @JsonProperty("target_type")
    private String targetType; // FIELD, JSONPATH, SCHEMA, ENDPOINT, COLUMN, TABLE, QUERY

    @JsonProperty("expected")
    private String expected;

    @JsonProperty("candidate")
    private String candidate;

    @JsonProperty("confidence_score")
    private double confidenceScore;

    @JsonProperty("confidence_level")
    private ConfidenceLevel confidenceLevel;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("validation_result")
    private String validationResult;

    @JsonProperty("failure_category")
    private FailureCategory failureCategory;

    @JsonProperty("details")
    private String details;

    @JsonProperty("timestamp")
    private String timestamp;

    public SelfHealingEvent() {
        this.timestamp = Instant.now().toString();
    }

    public SelfHealingEvent(
            String domain,
            String testName,
            String targetType,
            String expected,
            String candidate,
            double confidenceScore,
            ConfidenceLevel confidenceLevel,
            Status status,
            String validationResult,
            FailureCategory failureCategory,
            String details
    ) {
        this.domain = domain;
        this.testName = testName;
        this.targetType = targetType;
        this.expected = expected;
        this.candidate = candidate;
        this.confidenceScore = confidenceScore;
        this.confidenceLevel = confidenceLevel;
        this.status = status;
        this.validationResult = validationResult;
        this.failureCategory = failureCategory;
        this.details = details;
        this.timestamp = Instant.now().toString();
    }

    public String getDomain() {
        return domain;
    }

    public String getTestName() {
        return testName;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getExpected() {
        return expected;
    }

    public String getCandidate() {
        return candidate;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public int getConfidencePercentage() {
        return (int) Math.round(confidenceScore * 100);
    }

    public ConfidenceLevel getConfidenceLevel() {
        return confidenceLevel;
    }

    public Status getStatus() {
        return status;
    }

    public String getValidationResult() {
        return validationResult;
    }

    public FailureCategory getFailureCategory() {
        return failureCategory;
    }

    public String getDetails() {
        return details;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

