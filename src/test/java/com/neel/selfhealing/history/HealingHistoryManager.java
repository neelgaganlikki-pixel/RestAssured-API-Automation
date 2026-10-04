package com.neel.selfhealing.history;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.neel.selfhealing.config.SelfHealingConfig;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager responsible for persisting and loading self-healing history.
 * Ensures validated mappings can be recalled with high confidence in future test runs.
 */
public class HealingHistoryManager {

    private static volatile HealingHistoryManager instance;

    private final ObjectMapper objectMapper;
    private final Map<String, HealingRecord> historyMap = new ConcurrentHashMap<>();
    private final File historyFile;
    private final boolean saveEnabled;

    private HealingHistoryManager() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        SelfHealingConfig config = SelfHealingConfig.getInstance();
        this.saveEnabled = config.isSaveHistory();
        this.historyFile = new File(config.getHistoryFilePath());
        loadHistory();
    }

    public static HealingHistoryManager getInstance() {
        if (instance == null) {
            synchronized (HealingHistoryManager.class) {
                if (instance == null) {
                    instance = new HealingHistoryManager();
                }
            }
        }
        return instance;
    }

    public synchronized void recordSuccessfulHealing(
            String targetType,
            String originalKey,
            String healedKey,
            double confidence,
            String testName
    ) {
        String compositeKey = targetType + "." + originalKey;
        HealingRecord existing = historyMap.get(compositeKey);
        if (existing != null && existing.getHealed().equals(healedKey)) {
            existing.incrementSuccess();
            existing.setConfidence(Math.max(existing.getConfidence(), confidence));
            existing.setTestName(testName);
        } else {
            HealingRecord newRecord = new HealingRecord(
                    originalKey,
                    healedKey,
                    confidence,
                    1,
                    targetType,
                    testName
            );
            historyMap.put(compositeKey, newRecord);
        }

        if (saveEnabled) {
            saveHistory();
        }
    }

    public HealingRecord lookup(String targetType, String originalKey) {
        return historyMap.get(targetType + "." + originalKey);
    }

    public Map<String, HealingRecord> getAllHistory() {
        return new ConcurrentHashMap<>(historyMap);
    }

    private synchronized void loadHistory() {
        if (!historyFile.exists()) {
            return;
        }
        try {
            Map<String, HealingRecord> loaded = objectMapper.readValue(
                    historyFile,
                    new TypeReference<Map<String, HealingRecord>>() {}
            );
            if (loaded != null) {
                historyMap.putAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("[SELF-HEALING] Warning: Unable to parse healing history file: " + e.getMessage());
        }
    }

    public synchronized void saveHistory() {
        if (!saveEnabled) return;
        try {
            File parent = historyFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            objectMapper.writeValue(historyFile, historyMap);
        } catch (Exception e) {
            System.err.println("[SELF-HEALING] Warning: Failed to persist healing history: " + e.getMessage());
        }
    }

    public synchronized void clear() {
        historyMap.clear();
        if (historyFile.exists()) {
            historyFile.delete();
        }
    }
}

