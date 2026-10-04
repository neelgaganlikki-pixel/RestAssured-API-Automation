package com.neel.selfhealing.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neel.selfhealing.confidence.ConfidenceEngine;
import com.neel.selfhealing.confidence.ConfidenceLevel;
import com.neel.selfhealing.confidence.ConfidenceScore;
import com.neel.selfhealing.config.SelfHealingConfig;
import com.neel.selfhealing.report.SelfHealingEvent;
import com.neel.selfhealing.report.SelfHealingReporter;

import java.util.*;

/**
 * Controlled Schema Healer that detects structural differences between expected schema
 * and actual API responses, diagnoses mapping candidates, and evaluates confidence.
 */
public class SchemaHealer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConfidenceEngine confidenceEngine;

    public SchemaHealer(ConfidenceEngine confidenceEngine) {
        this.confidenceEngine = confidenceEngine;
    }

    public static class SchemaMappingDiagnostic {
        private final Map<String, String> fieldMappings = new LinkedHashMap<>();
        private final ConfidenceLevel confidenceLevel;
        private final double averageConfidence;
        private final boolean valid;

        public SchemaMappingDiagnostic(Map<String, String> fieldMappings, ConfidenceLevel confidenceLevel, double averageConfidence, boolean valid) {
            this.fieldMappings.putAll(fieldMappings);
            this.confidenceLevel = confidenceLevel;
            this.averageConfidence = averageConfidence;
            this.valid = valid;
        }

        public Map<String, String> getFieldMappings() {
            return fieldMappings;
        }

        public ConfidenceLevel getConfidenceLevel() {
            return confidenceLevel;
        }

        public double getAverageConfidence() {
            return averageConfidence;
        }

        public boolean isValid() {
            return valid;
        }
    }

    /**
     * Compares an expected schema map (field name -> expected type) against an actual JSON body.
     */
    public SchemaMappingDiagnostic analyzeSchema(
            String testName,
            Map<String, String> expectedSchema,
            String actualJson
    ) {
        if (!SelfHealingConfig.getInstance().isApiSelfHealingEnabled()) {
            return new SchemaMappingDiagnostic(Collections.emptyMap(), ConfidenceLevel.LOW, 0.0, false);
        }

        Map<String, String> candidateMappings = new LinkedHashMap<>();
        double totalConfidence = 0.0;
        int mappedCount = 0;

        try {
            JsonNode root = objectMapper.readTree(actualJson);
            // If response is an array, inspect the first item
            if (root.isArray() && root.size() > 0) {
                root = root.get(0);
            }

            if (!root.isObject()) {
                return new SchemaMappingDiagnostic(Collections.emptyMap(), ConfidenceLevel.LOW, 0.0, false);
            }

            // Extract actual fields and their types
            Map<String, String> actualFields = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                actualFields.put(entry.getKey(), deriveJsonNodeType(entry.getValue()));
            }

            for (Map.Entry<String, String> expEntry : expectedSchema.entrySet()) {
                String expField = expEntry.getKey();
                String expType = expEntry.getValue();

                if (actualFields.containsKey(expField)) {
                    // Exact match
                    candidateMappings.put(expField, expField);
                    totalConfidence += 1.0;
                    mappedCount++;
                } else {
                    // Search for candidate in actual fields
                    String bestCandidate = null;
                    ConfidenceScore bestScore = null;

                    for (Map.Entry<String, String> actEntry : actualFields.entrySet()) {
                        String actField = actEntry.getKey();
                        String actType = actEntry.getValue();

                        ConfidenceScore score = confidenceEngine.evaluate(
                                expField,
                                actField,
                                expType,
                                actType,
                                Collections.emptyMap(),
                                0
                        );

                        if (score.getOverallScore() >= SelfHealingConfig.getInstance().getConfidenceThreshold()) {
                            if (bestScore == null || score.getOverallScore() > bestScore.getOverallScore()) {
                                bestScore = score;
                                bestCandidate = actField;
                            }
                        }
                    }

                    if (bestCandidate != null && bestScore != null) {
                        candidateMappings.put(expField, bestCandidate);
                        totalConfidence += bestScore.getOverallScore();
                        mappedCount++;

                        SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                                "API",
                                testName,
                                "SCHEMA",
                                expField,
                                bestCandidate,
                                bestScore.getOverallScore(),
                                bestScore.getLevel(),
                                SelfHealingEvent.Status.HEALED,
                                "PASSED",
                                null,
                                "Schema field mapped: " + expField + " -> " + bestCandidate
                        ));
                    }
                }
            }

            double avgConfidence = mappedCount > 0 ? totalConfidence / mappedCount : 0.0;
            ConfidenceLevel level = ConfidenceLevel.fromScore(avgConfidence);

            // Log diagnostic as specified in Section 8
            System.out.println();
            System.out.println("[API-SELF-HEALING]");
            System.out.println("Possible schema mapping:");
            for (Map.Entry<String, String> entry : candidateMappings.entrySet()) {
                System.out.println(entry.getKey() + " -> " + entry.getValue());
            }
            System.out.println("Confidence: " + level);
            System.out.println("--------------------------------------------------");

            return new SchemaMappingDiagnostic(
                    candidateMappings,
                    level,
                    avgConfidence,
                    mappedCount == expectedSchema.size()
            );

        } catch (Exception e) {
            return new SchemaMappingDiagnostic(Collections.emptyMap(), ConfidenceLevel.LOW, 0.0, false);
        }
    }

    private String deriveJsonNodeType(JsonNode node) {
        if (node.isInt() || node.isLong() || node.isIntegralNumber()) return "INTEGER";
        if (node.isDouble() || node.isFloat() || node.isFloatingPointNumber()) return "NUMBER";
        if (node.isTextual()) return "STRING";
        if (node.isBoolean()) return "BOOLEAN";
        if (node.isArray()) return "ARRAY";
        if (node.isObject()) return "OBJECT";
        return "UNKNOWN";
    }
}

