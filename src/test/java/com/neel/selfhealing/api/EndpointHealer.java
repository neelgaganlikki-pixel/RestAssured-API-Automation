package com.neel.selfhealing.api;

import com.neel.selfhealing.confidence.ConfidenceEngine;
import com.neel.selfhealing.confidence.ConfidenceScore;
import com.neel.selfhealing.config.SelfHealingConfig;
import com.neel.selfhealing.report.SelfHealingEvent;
import com.neel.selfhealing.report.SelfHealingReporter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controlled Endpoint Healer.
 * Recovers from endpoint versioning changes (e.g., /api/v1/users -> /api/v2/users)
 * based strictly on registered known API routes or version migration rules.
 * Never performs uncontrolled network requests or brute-forcing.
 */
public class EndpointHealer {

    private static final Pattern VERSION_PATTERN = Pattern.compile("/v(\\d+)/");
    private final Set<String> knownRoutes = new HashSet<>();
    private final ConfidenceEngine confidenceEngine;

    public EndpointHealer(ConfidenceEngine confidenceEngine) {
        this.confidenceEngine = confidenceEngine;
    }

    public void registerKnownRoute(String route) {
        if (route != null) {
            knownRoutes.add(route.trim());
        }
    }

    public String healEndpoint(String testName, String originalEndpoint) {
        if (originalEndpoint == null || !SelfHealingConfig.getInstance().isApiSelfHealingEnabled()) {
            return originalEndpoint;
        }

        // 1. Check for standard version increment pattern (e.g., /v1/ -> /v2/)
        Matcher m = VERSION_PATTERN.matcher(originalEndpoint);
        if (m.find()) {
            int currentVer = Integer.parseInt(m.group(1));
            String candidateEndpoint = originalEndpoint.replaceFirst("/v" + currentVer + "/", "/v" + (currentVer + 1) + "/");

            ConfidenceScore score = confidenceEngine.evaluate(
                    originalEndpoint,
                    candidateEndpoint,
                    "ENDPOINT",
                    "ENDPOINT",
                    Collections.emptyMap(),
                    1
            );

            if (score.getOverallScore() >= SelfHealingConfig.getInstance().getConfidenceThreshold()) {
                SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                        "API",
                        testName,
                        "ENDPOINT",
                        originalEndpoint,
                        candidateEndpoint,
                        score.getOverallScore(),
                        score.getLevel(),
                        SelfHealingEvent.Status.HEALED,
                        "PASSED",
                        null,
                        "Endpoint version upgraded safely: " + originalEndpoint + " -> " + candidateEndpoint
                ));
                return candidateEndpoint;
            }
        }

        // 2. Search registered known routes
        String bestMatch = null;
        ConfidenceScore bestScore = null;
        for (String route : knownRoutes) {
            ConfidenceScore score = confidenceEngine.evaluate(
                    originalEndpoint,
                    route,
                    "ENDPOINT",
                    "ENDPOINT",
                    Collections.emptyMap(),
                    0
            );
            if (score.getOverallScore() >= SelfHealingConfig.getInstance().getConfidenceThreshold()) {
                if (bestScore == null || score.getOverallScore() > bestScore.getOverallScore()) {
                    bestScore = score;
                    bestMatch = route;
                }
            }
        }

        if (bestMatch != null && bestScore != null) {
            SelfHealingReporter.getInstance().recordEvent(new SelfHealingEvent(
                    "API",
                    testName,
                    "ENDPOINT",
                    originalEndpoint,
                    bestMatch,
                    bestScore.getOverallScore(),
                    bestScore.getLevel(),
                    SelfHealingEvent.Status.HEALED,
                    "PASSED",
                    null,
                    "Route matched with known API routes"
            ));
            return bestMatch;
        }

        // Safe failure: never guess arbitrarily
        return originalEndpoint;
    }
}

