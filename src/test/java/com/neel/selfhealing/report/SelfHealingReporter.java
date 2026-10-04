package com.neel.selfhealing.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.neel.selfhealing.config.SelfHealingConfig;
import com.neel.selfhealing.security.SecurityMasker;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Production Self-Healing Reporter responsible for logging events in standardized formats,
 * printing console summaries, and generating JSON and HTML reports.
 */
public class SelfHealingReporter {

    private static volatile SelfHealingReporter instance;

    private final SelfHealingReport report = new SelfHealingReport();
    private final ObjectMapper objectMapper;
    private final String reportDir;

    private SelfHealingReporter() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.reportDir = SelfHealingConfig.getInstance().getReportDirectory();
    }

    public static SelfHealingReporter getInstance() {
        if (instance == null) {
            synchronized (SelfHealingReporter.class) {
                if (instance == null) {
                    instance = new SelfHealingReporter();
                }
            }
        }
        return instance;
    }

    public synchronized void recordEvent(SelfHealingEvent event) {
        report.recordEvent(event);
    }

    public void logApiHealing(
            String testName,
            String expectedField,
            String detectedField,
            String valueType,
            int confidencePercent,
            String validationStatus,
            String healingStatus
    ) {
        System.out.println();
        System.out.println("[API-SELF-HEALING]");
        System.out.println("Test:");
        System.out.println(testName);
        System.out.println("Expected field:");
        System.out.println(expectedField);
        System.out.println("Candidate field:");
        System.out.println(detectedField);
        System.out.println("Value type:");
        System.out.println(valueType);
        System.out.println("Confidence:");
        System.out.println(confidencePercent + "%");
        System.out.println("Validation:");
        System.out.println(validationStatus);
        System.out.println("Healing:");
        System.out.println(healingStatus);
        System.out.println("--------------------------------------------------");
    }

    public void logDbHealing(
            String testName,
            String expectedColumn,
            String detectedColumn,
            String dataType,
            int confidencePercent,
            String validationStatus,
            String healingStatus
    ) {
        System.out.println();
        System.out.println("[DB-SELF-HEALING]");
        System.out.println("Test:");
        System.out.println(testName);
        System.out.println("Expected column:");
        System.out.println(expectedColumn);
        System.out.println("Detected column:");
        System.out.println(detectedColumn);
        System.out.println("Data type:");
        System.out.println(dataType);
        System.out.println("Confidence:");
        System.out.println(confidencePercent + "%");
        System.out.println("Validation:");
        System.out.println(validationStatus);
        System.out.println("Healing:");
        System.out.println(healingStatus);
        System.out.println("--------------------------------------------------");
    }

    public void printConsoleSummary() {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("              SELF-HEALING SUMMARY                ");
        System.out.println("==================================================");
        System.out.println("API:");
        System.out.println("  Attempts : " + report.getApiAttempts());
        System.out.println("  Healed   : " + report.getApiHealed());
        System.out.println("  Rejected : " + report.getApiRejected());
        System.out.println("  Failed   : " + report.getApiFailed());
        System.out.println();
        System.out.println("Database:");
        System.out.println("  Attempts : " + report.getDbAttempts());
        System.out.println("  Healed   : " + report.getDbHealed());
        System.out.println("  Rejected : " + report.getDbRejected());
        System.out.println("  Failed   : " + report.getDbFailed());
        System.out.println();
        System.out.println("Confidence Metrics:");
        System.out.println("  High Confidence Mappings : " + report.getHighConfidenceMappings());
        System.out.println("  Low Confidence Mappings  : " + report.getLowConfidenceMappings());
        System.out.println("==================================================");
        System.out.println();
    }

    public synchronized void generateReports() {
        File dir = new File(reportDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 1. Generate JSON report
        File jsonFile = new File(dir, "self-healing-report.json");
        try {
            objectMapper.writeValue(jsonFile, report);
        } catch (Exception e) {
            System.err.println("[SELF-HEALING] Error writing JSON report: " + e.getMessage());
        }

        // 2. Generate HTML report
        File htmlFile = new File(dir, "self-healing-report.html");
        try (PrintWriter writer = new PrintWriter(new FileWriter(htmlFile))) {
            writer.println(buildHtmlReport());
        } catch (Exception e) {
            System.err.println("[SELF-HEALING] Error writing HTML report: " + e.getMessage());
        }
    }

    private String buildHtmlReport() {
        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
          .append("<html lang=\"en\">\n<head>\n")
          .append("<meta charset=\"UTF-8\">\n")
          .append("<title>Self-Healing Test Automation Report</title>\n")
          .append("<style>\n")
          .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; margin: 0; padding: 20px; background: #f8fafc; color: #1e293b; }\n")
          .append(".container { max-width: 1200px; margin: 0 auto; background: #fff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }\n")
          .append("h1 { color: #0f172a; margin-top: 0; display: flex; align-items: center; justify-content: space-between; }\n")
          .append(".metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin: 24px 0; }\n")
          .append(".metric-card { background: #f1f5f9; border-radius: 6px; padding: 18px; border-left: 4px solid #3b82f6; }\n")
          .append(".metric-card.api { border-left-color: #06b6d4; }\n")
          .append(".metric-card.db { border-left-color: #8b5cf6; }\n")
          .append(".metric-card.success { border-left-color: #10b981; }\n")
          .append(".metric-card.rejected { border-left-color: #f59e0b; }\n")
          .append(".metric-value { font-size: 28px; font-weight: 700; margin: 8px 0 0; }\n")
          .append(".metric-title { font-size: 13px; text-transform: uppercase; color: #64748b; letter-spacing: 0.5px; }\n")
          .append("table { width: 100%; border-collapse: collapse; margin-top: 24px; }\n")
          .append("th, td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #e2e8f0; font-size: 14px; }\n")
          .append("th { background: #f8fafc; color: #475569; font-weight: 600; text-transform: uppercase; font-size: 12px; }\n")
          .append("tr:hover { background: #f8fafc; }\n")
          .append(".badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; display: inline-block; }\n")
          .append(".badge-healed { background: #dcfce7; color: #166534; }\n")
          .append(".badge-rejected { background: #fef3c7; color: #92400e; }\n")
          .append(".badge-failed { background: #fee2e2; color: #991b1b; }\n")
          .append(".badge-api { background: #e0f2fe; color: #0369a1; }\n")
          .append(".badge-db { background: #f3e8ff; color: #6b21a8; }\n")
          .append("</style>\n</head>\n<body>\n")
          .append("<div class=\"container\">\n")
          .append("<h1><span>Self-Healing Automation Report</span><span style=\"font-size:14px;font-weight:normal;color:#64748b;\">Generated: ").append(generatedAt).append("</span></h1>\n")
          .append("<div class=\"metrics-grid\">\n")
          .append("  <div class=\"metric-card api\"><div class=\"metric-title\">API Total Attempts</div><div class=\"metric-value\">").append(report.getApiAttempts()).append("</div></div>\n")
          .append("  <div class=\"metric-card success\"><div class=\"metric-title\">API Healed</div><div class=\"metric-value\">").append(report.getApiHealed()).append("</div></div>\n")
          .append("  <div class=\"metric-card rejected\"><div class=\"metric-title\">API Rejected</div><div class=\"metric-value\">").append(report.getApiRejected()).append("</div></div>\n")
          .append("  <div class=\"metric-card db\"><div class=\"metric-title\">DB Total Attempts</div><div class=\"metric-value\">").append(report.getDbAttempts()).append("</div></div>\n")
          .append("  <div class=\"metric-card success\"><div class=\"metric-title\">DB Healed</div><div class=\"metric-value\">").append(report.getDbHealed()).append("</div></div>\n")
          .append("  <div class=\"metric-card rejected\"><div class=\"metric-title\">DB Rejected</div><div class=\"metric-value\">").append(report.getDbRejected()).append("</div></div>\n")
          .append("</div>\n")
          .append("<h3>Detailed Healing Events</h3>\n")
          .append("<table>\n<thead>\n<tr>")
          .append("<th>Domain</th><th>Test Name</th><th>Target</th><th>Expected</th><th>Detected</th><th>Confidence</th><th>Status</th><th>Validation</th>")
          .append("</tr>\n</thead>\n<tbody>\n");

        for (SelfHealingEvent event : report.getEvents()) {
            String domainBadge = "API".equalsIgnoreCase(event.getDomain()) ? "badge-api" : "badge-db";
            String statusBadge = event.getStatus() == SelfHealingEvent.Status.HEALED ? "badge-healed" :
                                 event.getStatus() == SelfHealingEvent.Status.REJECTED ? "badge-rejected" : "badge-failed";

            sb.append("<tr>\n")
              .append("<td><span class=\"badge ").append(domainBadge).append("\">").append(escapeHtml(event.getDomain())).append("</span></td>\n")
              .append("<td>").append(escapeHtml(event.getTestName())).append("</td>\n")
              .append("<td>").append(escapeHtml(event.getTargetType())).append("</td>\n")
              .append("<td><code>").append(escapeHtml(SecurityMasker.mask(event.getExpected()))).append("</code></td>\n")
              .append("<td><code>").append(escapeHtml(SecurityMasker.mask(event.getCandidate()))).append("</code></td>\n")
              .append("<td><strong>").append(event.getConfidencePercentage()).append("%</strong> (").append(event.getConfidenceLevel()).append(")</td>\n")
              .append("<td><span class=\"badge ").append(statusBadge).append("\">").append(event.getStatus()).append("</span></td>\n")
              .append("<td>").append(escapeHtml(event.getValidationResult())).append("</td>\n")
              .append("</tr>\n");
        }

        sb.append("</tbody>\n</table>\n")
          .append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }

    public SelfHealingReport getReport() {
        return report;
    }
}

