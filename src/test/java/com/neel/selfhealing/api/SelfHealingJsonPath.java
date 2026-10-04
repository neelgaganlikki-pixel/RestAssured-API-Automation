package com.neel.selfhealing.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.path.json.JsonPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Intelligent JsonPath implementation with automatic self-healing fallback.
 * First delegates to REST Assured JsonPath.
 * If the path fails or is null, attempts controlled healing via ApiHealingEngine.
 */
public class SelfHealingJsonPath {

    private final String rawJson;
    private final JsonPath delegateJsonPath;
    private final ApiHealingEngine healingEngine;
    private final String testName;

    public SelfHealingJsonPath(String rawJson, String testName) {
        this.rawJson = rawJson;
        this.delegateJsonPath = rawJson != null && !rawJson.isEmpty() ? new JsonPath(rawJson) : null;
        this.healingEngine = ApiHealingEngine.getInstance();
        this.testName = testName != null ? testName : "ApiTest";
    }

    public static SelfHealingJsonPath from(String rawJson, String testName) {
        return new SelfHealingJsonPath(rawJson, testName);
    }

    public String getString(String path) {
        // 1. Try standard JsonPath
        try {
            if (delegateJsonPath != null) {
                Object val = delegateJsonPath.get(path);
                if (val != null) {
                    return String.valueOf(val);
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Self-healing fallback
        JsonNode healed = healingEngine.resolveField(testName, rawJson, path, "STRING");
        if (healed != null) {
            return healed.isTextual() ? healed.asText() : healed.toString();
        }

        return null;
    }

    public int getInt(String path) {
        // 1. Try standard JsonPath
        try {
            if (delegateJsonPath != null) {
                Object val = delegateJsonPath.get(path);
                if (val instanceof Number) {
                    return ((Number) val).intValue();
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Self-healing fallback
        JsonNode healed = healingEngine.resolveField(testName, rawJson, path, "INTEGER");
        if (healed != null && healed.isNumber()) {
            return healed.asInt();
        }

        throw new IllegalArgumentException("Cannot find or heal integer at path: " + path);
    }

    public boolean getBoolean(String path) {
        try {
            if (delegateJsonPath != null) {
                Object val = delegateJsonPath.get(path);
                if (val instanceof Boolean) {
                    return (Boolean) val;
                }
            }
        } catch (Exception ignored) {
        }

        JsonNode healed = healingEngine.resolveField(testName, rawJson, path, "BOOLEAN");
        if (healed != null && healed.isBoolean()) {
            return healed.asBoolean();
        }

        throw new IllegalArgumentException("Cannot find or heal boolean at path: " + path);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String path) {
        try {
            if (delegateJsonPath != null) {
                List<T> list = delegateJsonPath.getList(path);
                if (list != null && !list.isEmpty()) {
                    return list;
                }
            }
        } catch (Exception ignored) {
        }

        JsonNode healed = healingEngine.resolveField(testName, rawJson, path, "ARRAY");
        if (healed != null && healed.isArray()) {
            List<Object> result = new ArrayList<>();
            for (JsonNode item : healed) {
                if (item.isTextual()) result.add(item.asText());
                else if (item.isIntegralNumber()) result.add(item.asInt());
                else if (item.isBoolean()) result.add(item.asBoolean());
                else result.add(item.toString());
            }
            return (List<T>) result;
        }

        return delegateJsonPath != null ? delegateJsonPath.getList(path) : new ArrayList<>();
    }

    public Object get(String path) {
        String str = getString(path);
        if (str != null) return str;
        return delegateJsonPath != null ? delegateJsonPath.get(path) : null;
    }
}

