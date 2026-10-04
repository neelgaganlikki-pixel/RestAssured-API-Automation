package com.neel.selfhealing.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Production configuration for the Self-Healing Automation Engine.
 * Supports priority lookup:
 * 1. Java System Properties (-DSELF_HEALING_ENABLED=true)
 * 2. OS Environment Variables (SELF_HEALING_ENABLED=true)
 * 3. Classpath / File properties (self-healing.properties)
 * 4. Safe defaults
 */
public class SelfHealingConfig {

    private static final String DEFAULT_PROPERTIES_FILE = "self-healing.properties";

    private static volatile SelfHealingConfig instance;

    private final Properties properties = new Properties();

    private final boolean selfHealingEnabled;
    private final int maxAttempts;
    private final double confidenceThreshold;
    private final boolean saveHistory;
    private final String logLevel;
    private final boolean apiSelfHealingEnabled;
    private final boolean dbSelfHealingEnabled;
    private final String historyFilePath;
    private final String reportDirectory;

    private SelfHealingConfig() {
        loadProperties();

        this.selfHealingEnabled = getBooleanProperty("SELF_HEALING_ENABLED", true);
        this.maxAttempts = getIntProperty("SELF_HEALING_MAX_ATTEMPTS", 3);
        this.confidenceThreshold = getDoubleProperty("SELF_HEALING_CONFIDENCE_THRESHOLD", 0.90);
        this.saveHistory = getBooleanProperty("SELF_HEALING_SAVE_HISTORY", true);
        this.logLevel = getStringProperty("SELF_HEALING_LOG_LEVEL", "INFO");
        this.apiSelfHealingEnabled = getBooleanProperty("API_SELF_HEALING_ENABLED", true);
        this.dbSelfHealingEnabled = getBooleanProperty("DB_SELF_HEALING_ENABLED", true);
        this.historyFilePath = getStringProperty("SELF_HEALING_HISTORY_FILE", "reports/healing_history.json");
        this.reportDirectory = getStringProperty("SELF_HEALING_REPORT_DIR", "reports");
    }

    public static SelfHealingConfig getInstance() {
        if (instance == null) {
            synchronized (SelfHealingConfig.class) {
                if (instance == null) {
                    instance = new SelfHealingConfig();
                }
            }
        }
        return instance;
    }

    public static synchronized void reload() {
        instance = new SelfHealingConfig();
    }

    private void loadProperties() {
        // 1. Try classpath resource
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (Exception ignored) {
        }

        // 2. Try file system relative path
        File file = new File(DEFAULT_PROPERTIES_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (Exception ignored) {
            }
        }
    }

    private String getStringProperty(String key, String defaultValue) {
        // System property
        String val = System.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        // Env var
        val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        // Properties file
        val = properties.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        return defaultValue;
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String val = getStringProperty(key, null);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return defaultValue;
    }

    private int getIntProperty(String key, int defaultValue) {
        String val = getStringProperty(key, null);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private double getDoubleProperty(String key, double defaultValue) {
        String val = getStringProperty(key, null);
        if (val != null) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    public boolean isSelfHealingEnabled() {
        return selfHealingEnabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public boolean isSaveHistory() {
        return saveHistory;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public boolean isApiSelfHealingEnabled() {
        return selfHealingEnabled && apiSelfHealingEnabled;
    }

    public boolean isDbSelfHealingEnabled() {
        return selfHealingEnabled && dbSelfHealingEnabled;
    }

    public String getHistoryFilePath() {
        return historyFilePath;
    }

    public String getReportDirectory() {
        return reportDirectory;
    }
}

