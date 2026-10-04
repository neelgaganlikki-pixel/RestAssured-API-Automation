package com.neel.selfhealing.confidence;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable score breakdown representing multi-factor confidence evaluation.
 */
public class ConfidenceScore {

    private final double overallScore;
    private final ConfidenceLevel level;
    private final double nameSimilarity;
    private final double semanticSimilarity;
    private final double typeMatch;
    private final double contextMatch;
    private final double relationshipMultiplier;
    private final double historicalMultiplier;
    private final Map<String, Object> details;

    public ConfidenceScore(
            double overallScore,
            ConfidenceLevel level,
            double nameSimilarity,
            double semanticSimilarity,
            double typeMatch,
            double contextMatch,
            double relationshipMultiplier,
            double historicalMultiplier,
            Map<String, Object> details
    ) {
        this.overallScore = Math.max(0.0, Math.min(1.0, overallScore));
        this.level = level;
        this.nameSimilarity = nameSimilarity;
        this.semanticSimilarity = semanticSimilarity;
        this.typeMatch = typeMatch;
        this.contextMatch = contextMatch;
        this.relationshipMultiplier = relationshipMultiplier;
        this.historicalMultiplier = historicalMultiplier;
        this.details = details != null ? Collections.unmodifiableMap(details) : Collections.emptyMap();
    }

    public double getOverallScore() {
        return overallScore;
    }

    public int getPercentage() {
        return (int) Math.round(overallScore * 100);
    }

    public ConfidenceLevel getLevel() {
        return level;
    }

    public double getNameSimilarity() {
        return nameSimilarity;
    }

    public double getSemanticSimilarity() {
        return semanticSimilarity;
    }

    public double getTypeMatch() {
        return typeMatch;
    }

    public double getContextMatch() {
        return contextMatch;
    }

    public double getRelationshipMultiplier() {
        return relationshipMultiplier;
    }

    public double getHistoricalMultiplier() {
        return historicalMultiplier;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return String.format("%s (%d%%)", level, getPercentage());
    }
}

