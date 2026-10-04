package com.neel.selfhealing.confidence;

/**
 * Categorical confidence rating for self-healing candidate decisions.
 */
public enum ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW;

    public static ConfidenceLevel fromScore(double score) {
        if (score >= 0.90) {
            return HIGH;
        } else if (score >= 0.75) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }
}

