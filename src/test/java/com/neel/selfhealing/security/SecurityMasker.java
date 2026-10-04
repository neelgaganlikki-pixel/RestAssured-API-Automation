package com.neel.selfhealing.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Security masker to ensure secrets, tokens, credentials, and sensitive
 * information are never logged, stored in history, or displayed in reports.
 */
public final class SecurityMasker {

    private static final String MASK = "********";

    // Common sensitive keys in JSON, query params, headers, SQL
    private static final Pattern SENSITIVE_JSON_PATTERN = Pattern.compile(
            "\"(?i)(password|secret|token|access_token|refresh_token|apikey|api_key|authorization|auth|credential|card_number|cvv|private_key)\"\\s*:\\s*\"([^\"]+)\""
    );

    private static final Pattern BEARER_AUTH_PATTERN = Pattern.compile(
            "(?i)bearer\\s+[a-zA-Z0-9_\\-\\.]+"
    );

    private static final Pattern BASIC_AUTH_PATTERN = Pattern.compile(
            "(?i)basic\\s+[a-zA-Z0-9+/=]+"
    );

    private static final Pattern JDBC_CREDENTIAL_PATTERN = Pattern.compile(
            "(?i)(password|pwd|user|username)\\s*=\\s*([^;&]+)"
    );

    private SecurityMasker() {
    }

    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String masked = input;

        // 1. Mask JSON sensitive fields
        Matcher jsonMatcher = SENSITIVE_JSON_PATTERN.matcher(masked);
        StringBuffer sb = new StringBuffer();
        while (jsonMatcher.find()) {
            jsonMatcher.appendReplacement(sb, "\"" + jsonMatcher.group(1) + "\":\"" + MASK + "\"");
        }
        jsonMatcher.appendTail(sb);
        masked = sb.toString();

        // 2. Mask Bearer auth tokens
        masked = BEARER_AUTH_PATTERN.matcher(masked).replaceAll("Bearer " + MASK);

        // 3. Mask Basic auth
        masked = BASIC_AUTH_PATTERN.matcher(masked).replaceAll("Basic " + MASK);

        // 4. Mask JDBC connection string credentials
        Matcher jdbcMatcher = JDBC_CREDENTIAL_PATTERN.matcher(masked);
        sb = new StringBuffer();
        while (jdbcMatcher.find()) {
            jdbcMatcher.appendReplacement(sb, jdbcMatcher.group(1) + "=" + MASK);
        }
        jdbcMatcher.appendTail(sb);
        masked = sb.toString();

        return masked;
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return lower.contains("password")
                || lower.contains("secret")
                || lower.contains("token")
                || lower.contains("apikey")
                || lower.contains("api_key")
                || lower.contains("authorization")
                || lower.contains("credential")
                || lower.contains("privatekey");
    }
}

