package com.neel.selfhealing.history;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Historical record of a validated self-healing mapping.
 * Stored persistently in reports/healing_history.json.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealingRecord {

    @JsonProperty("original")
    private String original;

    @JsonProperty("healed")
    private String healed;

    @JsonProperty("confidence")
    private double confidence;

    @JsonProperty("success_count")
    private int successCount;

    @JsonProperty("target_type")
    private String targetType; // "API" or "DATABASE"

    @JsonProperty("test_name")
    private String testName;

    @JsonProperty("last_healed_at")
    private String lastHealedAt;

    public HealingRecord() {
    }

    public HealingRecord(
            String original,
            String healed,
            double confidence,
            int successCount,
            String targetType,
            String testName
    ) {
        this.original = original;
        this.healed = healed;
        this.confidence = confidence;
        this.successCount = successCount;
        this.targetType = targetType;
        this.testName = testName;
        this.lastHealedAt = Instant.now().toString();
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public String getHealed() {
        return healed;
    }

    public void setHealed(String healed) {
        this.healed = healed;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getLastHealedAt() {
        return lastHealedAt;
    }

    public void setLastHealedAt(String lastHealedAt) {
        this.lastHealedAt = lastHealedAt;
    }

    public void incrementSuccess() {
        this.successCount++;
        this.lastHealedAt = Instant.now().toString();
    }
}

