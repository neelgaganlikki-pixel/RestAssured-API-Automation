package com.neel.selfhealing.engine;

import com.neel.selfhealing.api.ApiHealingEngine;
import com.neel.selfhealing.config.SelfHealingConfig;
import com.neel.selfhealing.db.DatabaseHealingEngine;
import com.neel.selfhealing.history.HealingHistoryManager;
import com.neel.selfhealing.report.SelfHealingReporter;

/**
 * Unified Facade for the Production Self-Healing Automation System.
 */
public class SelfHealingEngine {

    private static volatile SelfHealingEngine instance;

    private final SelfHealingConfig config;
    private final ApiHealingEngine apiEngine;
    private final DatabaseHealingEngine dbEngine;
    private final HealingHistoryManager historyManager;
    private final SelfHealingReporter reporter;

    private SelfHealingEngine() {
        this.config = SelfHealingConfig.getInstance();
        this.apiEngine = ApiHealingEngine.getInstance();
        this.dbEngine = DatabaseHealingEngine.getInstance();
        this.historyManager = HealingHistoryManager.getInstance();
        this.reporter = SelfHealingReporter.getInstance();
    }

    public static SelfHealingEngine getInstance() {
        if (instance == null) {
            synchronized (SelfHealingEngine.class) {
                if (instance == null) {
                    instance = new SelfHealingEngine();
                }
            }
        }
        return instance;
    }

    public static void initialize() {
        getInstance();
        System.out.println("[SELF-HEALING] Production Self-Healing Automation Engine initialized.");
    }

    private static volatile boolean shutdownCompleted = false;

    public static synchronized void shutdown() {
        if (shutdownCompleted) return;
        shutdownCompleted = true;
        SelfHealingReporter rep = SelfHealingReporter.getInstance();
        rep.printConsoleSummary();
        rep.generateReports();
        HealingHistoryManager.getInstance().saveHistory();
    }

    public SelfHealingConfig getConfig() {
        return config;
    }

    public ApiHealingEngine getApiEngine() {
        return apiEngine;
    }

    public DatabaseHealingEngine getDbEngine() {
        return dbEngine;
    }

    public HealingHistoryManager getHistoryManager() {
        return historyManager;
    }

    public SelfHealingReporter getReporter() {
        return reporter;
    }
}
