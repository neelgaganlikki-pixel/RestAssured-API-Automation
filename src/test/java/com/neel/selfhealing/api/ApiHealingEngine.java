package com.neel.selfhealing.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neel.selfhealing.confidence.ConfidenceEngine;
import com.neel.selfhealing.confidence.ConfidenceLevel;
import com.neel.selfhealing.confidence.ConfidenceScore;
import com.neel.selfhealing.config.SelfHealingConfig;
import com.neel.selfhealing.history.HealingHistoryManager;
import com.neel.selfhealing.history.HealingRecord;
import com.neel.selfhealing.report.FailureCategory;
import com.neel.selfhealing.report.SelfHealingEvent;
import com.neel.selfhealing.report.SelfHealingReporter;
import com.neel.selfhealing.security.SecurityMasker;

import java.util.*;

/**
 * Production Engine for API Self-Healing Automation.
 * Recovers from renamed fields, modified JSONPaths, schema mutations, and request structures
 * while strictly failing when genuine defects or data inconsistencies are identified.
 */
public class ApiHealingEngine {

    private static volatile ApiHealingEngine instance;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConfidenceEngine confidenceEngine = new ConfidenceEngine();
    private final HealingHistoryManager historyManager = HealingHistoryManager.getInstance();
    private final EndpointHealer endpointHealer = new EndpointHealer(confidenceEngine);
    private final SchemaHealer schemaHealer = new SchemaHealer(confidenceEngine);

    private ApiHealingEngine() {
    }

    public static ApiHealingEngine getInstance() {
        if (instance == null) {
            synchronized (ApiHealingEngine.class) {
                if (instance == null) {
                    instance = new ApiHealingEngine();
                }
            }
        }
        return instance;
    }

    public EndpointHealer getEndpointHealer() {
        return endpointHealer;
    }

    public SchemaHealer getSchemaHealer() {
        return schemaHealer;
    }

    public ConfidenceEngine getConfidenceEngine() {
        return confidenceEngine;
    }

    /**
     * Resolves a field or JSONPath in the given JSON string with self-healing capabilities.
     *
     * @param testName     Name of the active test
     * @param jsonResponse Raw JSON response body
     * @param targetPath   Expected field name or JSONPath (e.g., "userName", "$.data.user.name")
     * @param expectedType Expected type hint ("STRING", "INTEGER", "BOOLEAN", etc.)
     * @return Healed JsonNode or null if not found/healed
     */
    public JsonNode resolveField(
            String testName,
            String jsonResponse,
            String targetPath,
            String expectedType
    ) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty() || targetPath == null) {
            return null;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(jsonResponse);
        } catch (Exception e) {
            return null;
        }

        // 1. Direct path lookup (clean standard behavior, no healing needed)
        JsonNode directNode = navigatePath(root, targetPath);
        if (directNode != null && !directNode.isMissingNode() && !directNode.isNull()) {
            return directNode;
        }

        // If self-healing is disabled, do not attempt to heal
        if (!SelfHealingConfig.getInstance().isApiSelfHealingEnabled()) {
            return null;
        }

        // 2. Check persistent healing history
        String normalizedTarget = cleanPath(targetPath);
        HealingRecord cached = historyManager.lookup("API", normalizedTarget);
        if (cached != null && cached.getConfidence() >= SelfHealingConfig.getInstance().getConfidenceThreshold()) {
            JsonNode historyNode = navigatePath(root, cached.getHealed());
            if (historyNode != null && !historyNode.isMissingNode()) {
                historyManager.recordSuccessfulHealing(
                        "API",
                        normalizedTarget,
                        cached.getHealed(),
                        cached.getConfidence(),
                        testName
                );
                SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                        "API",
                        testName,
                        "FIELD",
                        targetPath,
                        cached.getHealed(),
                        cached.getConfidence(),
                        ConfidenceLevel.fromScore(cached.getConfidence()),
                        SelfHealingEvent.Status.HEALED,
                        "PASSED",
                        null,
                        "Field healed from history: " + targetPath + " -> " + cached.getHealed()
                ));
                return historyNode;
            }
        }

        // 3. Search candidate fields across the AST
        Map<String, JsonNode> candidateMap = new LinkedHashMap<>();
        collectAllFieldPaths(root, "", candidateMap);

        String bestCandidateKey = null;
        JsonNode bestCandidateNode = null;
        ConfidenceScore bestScore = null;

        for (Map.Entry<String, JsonNode> entry : candidateMap.entrySet()) {
            String candidatePath = entry.getKey();
            JsonNode candidateNode = entry.getValue();
            String candidateLeafName = extractLeaf(candidatePath);
            String candidateType = deriveType(candidateNode);

            // Evaluate leaf name similarity and full path structural similarity
            ConfidenceScore score = confidenceEngine.evaluate(
                    extractLeaf(targetPath),
                    candidateLeafName,
                    expectedType,
                    candidateType,
                    Collections.emptyMap(),
                    0
            );

            if (score.getOverallScore() >= SelfHealingConfig.getInstance().getConfidenceThreshold()) {
                if (bestScore == null || score.getOverallScore() > bestScore.getOverallScore()) {
                    bestScore = score;
                    bestCandidateKey = candidatePath;
                    bestCandidateNode = candidateNode;
                }
            }
        }

        // 4. Decision: Heal if confidence threshold is met, otherwise reject
        if (bestCandidateKey != null && bestCandidateNode != null && bestScore != null) {
            int confidencePercent = bestScore.getPercentage();
            String valueType = deriveType(bestCandidateNode);

            // Log according to prompt Sections 6 & 22
            SelfHealingReporter.getInstance().logApiHealing(
                    testName,
                    targetPath,
                    bestCandidateKey,
                    valueType,
                    confidencePercent,
                    "PASSED",
                    "SUCCESS"
            );

            // Record event
            SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                    "API",
                    testName,
                    "FIELD",
                    targetPath,
                    bestCandidateKey,
                    bestScore.getOverallScore(),
                    bestScore.getLevel(),
                    SelfHealingEvent.Status.HEALED,
                    "PASSED",
                    null,
                    "Field recovered with high confidence: " + targetPath + " -> " + bestCandidateKey
            ));

            // Record history
            historyManager.recordSuccessfulHealing(
                    "API",
                    normalizedTarget,
                    bestCandidateKey,
                    bestScore.getOverallScore(),
                    testName
            );

            return bestCandidateNode;
        } else {
            // Failed/Rejected healing attempt
            SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                    "API",
                    testName,
                    "FIELD",
                    targetPath,
                    "NONE",
                    0.0,
                    ConfidenceLevel.LOW,
                    SelfHealingEvent.Status.REJECTED,
                    "FAILED",
                    FailureCategory.API_LOCATOR_MAPPING_FAILURE,
                    "No candidate field met the confidence threshold (>= " + (int)(SelfHealingConfig.getInstance().getConfidenceThreshold() * 100) + "%)"
            ));
            return null;
        }
    }

    /**
     * Request healing: heals request body payload keys before sending.
     */
    public String healRequestBody(String testName, String requestJson, Map<String, String> expectedToActualMappings) {
        if (requestJson == null || !SelfHealingConfig.getInstance().isApiSelfHealingEnabled() || expectedToActualMappings == null) {
            return requestJson;
        }

        try {
            JsonNode root = objectMapper.readTree(requestJson);
            if (!root.isObject()) return requestJson;

            ObjectNode obj = (ObjectNode) root;
            boolean modified = false;

            for (Map.Entry<String, String> entry : expectedToActualMappings.entrySet()) {
                String oldKey = entry.getKey();
                String newKey = entry.getValue();

                if (obj.has(oldKey)) {
                    // Do not heal sensitive fields
                    if (SecurityMasker.isSensitiveKey(oldKey) || SecurityMasker.isSensitiveKey(newKey)) {
                        continue;
                    }
                    JsonNode val = obj.remove(oldKey);
                    obj.set(newKey, val);
                    modified = true;

                    SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                            "API",
                            testName,
                            "REQUEST_FIELD",
                            oldKey,
                            newKey,
                            0.95,
                            ConfidenceLevel.HIGH,
                            SelfHealingEvent.Status.HEALED,
                            "PASSED",
                            null,
                            "Request field healed: " + oldKey + " -> " + newKey
                    ));
                }
            }

            return modified ? objectMapper.writeValueAsString(obj) : requestJson;
        } catch (Exception e) {
            return requestJson;
        }
    }

    private JsonNode navigatePath(JsonNode root, String path) {
        String clean = cleanPath(path);
        String[] tokens = clean.split("\\.");
        JsonNode current = root;

        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (current == null || current.isMissingNode()) return null;

            if (token.contains("[") && token.endsWith("]")) {
                int bracketIdx = token.indexOf('[');
                String fieldName = token.substring(0, bracketIdx);
                int arrayIdx = Integer.parseInt(token.substring(bracketIdx + 1, token.length() - 1));

                if (!fieldName.isEmpty()) {
                    current = current.path(fieldName);
                }
                current = current.path(arrayIdx);
            } else {
                current = current.path(token);
            }
        }

        return (current != null && !current.isMissingNode()) ? current : null;
    }

    private void collectAllFieldPaths(JsonNode node, String currentPath, Map<String, JsonNode> results) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String path = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();
                results.put(path, entry.getValue());
                collectAllFieldPaths(entry.getValue(), path, results);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String path = currentPath + "[" + i + "]";
                results.put(path, node.get(i));
                collectAllFieldPaths(node.get(i), path, results);
            }
        }
    }

    private String cleanPath(String path) {
        if (path == null) return "";
        String s = path.trim();
        if (s.startsWith("$.")) s = s.substring(2);
        else if (s.startsWith("$")) s = s.substring(1);
        if (s.startsWith(".")) s = s.substring(1);
        return s;
    }

    private String extractLeaf(String path) {
        String clean = cleanPath(path);
        int lastDot = clean.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < clean.length() - 1) {
            clean = clean.substring(lastDot + 1);
        }
        int bracket = clean.indexOf('[');
        if (bracket > 0) {
            clean = clean.substring(0, bracket);
        }
        return clean;
    }

    private String deriveType(JsonNode node) {
        if (node == null) return "UNKNOWN";
        if (node.isInt() || node.isLong() || node.isIntegralNumber()) return "INTEGER";
        if (node.isDouble() || node.isFloat() || node.isFloatingPointNumber()) return "NUMBER";
        if (node.isTextual()) return "STRING";
        if (node.isBoolean()) return "BOOLEAN";
        if (node.isArray()) return "ARRAY";
        if (node.isObject()) return "OBJECT";
        return "UNKNOWN";
    }
}

