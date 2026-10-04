package com.neel.selfhealing.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Report model containing aggregated statistics and all self-healing events.
 */
public class SelfHealingReport {

    @JsonProperty("api_attempts")
    private int apiAttempts = 0;

    @JsonProperty("api_healed")
    private int apiHealed = 0;

    @JsonProperty("api_rejected")
    private int apiRejected = 0;

    @JsonProperty("api_failed")
    private int apiFailed = 0;

    @JsonProperty("db_attempts")
    private int dbAttempts = 0;

    @JsonProperty("db_healed")
    private int dbHealed = 0;

    @JsonProperty("db_rejected")
    private int dbRejected = 0;

    @JsonProperty("db_failed")
    private int dbFailed = 0;

    @JsonProperty("high_confidence_mappings")
    private int highConfidenceMappings = 0;

    @JsonProperty("low_confidence_mappings")
    private int lowConfidenceMappings = 0;

    @JsonProperty("events")
    private final List<SelfHealingEvent> events = new ArrayList<>();

    public synchronized void recordEvent(SelfHealingEvent event) {
        events.add(event);

        boolean isApi = "API".equalsIgnoreCase(event.getDomain());
        if (isApi) {
            apiAttempts++;
            if (event.getStatus() == SelfHealingEvent.Status.HEALED) {
                apiHealed++;
            } else if (event.getStatus() == SelfHealingEvent.Status.REJECTED) {
                apiRejected++;
            } else {
                apiFailed++;
            }
        } else {
            dbAttempts++;
            if (event.getStatus() == SelfHealingEvent.Status.HEALED) {
                dbHealed++;
            } else if (event.getStatus() == SelfHealingEvent.Status.REJECTED) {
                dbRejected++;
            } else {
                dbFailed++;
            }
        }

        if (event.getConfidenceScore() >= 0.90) {
            highConfidenceMappings++;
        } else {
            lowConfidenceMappings++;
        }
    }

    public int getApiAttempts() {
        return apiAttempts;
    }

    public int getApiHealed() {
        return apiHealed;
    }

    public int getApiRejected() {
        return apiRejected;
    }

    public int getApiFailed() {
        return apiFailed;
    }

    public int getDbAttempts() {
        return dbAttempts;
    }

    public int getDbHealed() {
        return dbHealed;
    }

    public int getDbRejected() {
        return dbRejected;
    }

    public int getDbFailed() {
        return dbFailed;
    }

    public int getTotalAttempts() {
        return apiAttempts + dbAttempts;
    }

    public int getTotalHealed() {
        return apiHealed + dbHealed;
    }

    public int getTotalRejected() {
        return apiRejected + dbRejected;
    }

    public int getHighConfidenceMappings() {
        return highConfidenceMappings;
    }

    public int getLowConfidenceMappings() {
        return lowConfidenceMappings;
    }

    public List<SelfHealingEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }
}

