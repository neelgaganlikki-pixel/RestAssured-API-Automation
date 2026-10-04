package com.neel.selfhealing.confidence;

import java.util.*;

/**
 * Intelligent Multi-Factor Confidence Engine for API and Database Self-Healing.
 * Evaluates:
 * 1. Normalized Syntactic Name Similarity (Levenshtein + Jaro-Winkler + Token Match)
 * 2. Semantic Dictionary & Synonym Alignment
 * 3. Strict Data Type Compatibility (Java / SQL / JSON types)
 * 4. Structural Context (Parent object, Sibling fields, Table structure, Table-prefixed column attributes)
 * 5. Relationship Awareness (Entity identity preservation, e.g. customer_id != product_id)
 * 6. Historical Success Weighting
 */
public class ConfidenceEngine {

    // Synonym clusters for API and Database fields
    private static final Map<String, Set<String>> SYNONYM_MAP = new HashMap<>();

    // Domain entity markers to prevent cross-entity mapping (e.g., customer vs product vs order)
    private static final Set<String> ENTITY_ROOTS = new HashSet<>(Arrays.asList(
            "customer", "user", "client", "product", "item", "order", "invoice", "payment",
            "account", "employee", "vendor", "supplier", "address", "geo", "company"
    ));

    static {
        registerSynonymGroup("id", "identifier", "key", "pk", "user_id", "customer_id", "cust_id", "order_id");
        registerSynonymGroup("username", "user_name", "user", "login", "account_name");
        registerSynonymGroup("name", "full_name", "display_name", "cust_name", "customer_name", "customername");
        registerSynonymGroup("email", "email_address", "mail", "e_mail");
        registerSynonymGroup("phone", "phone_number", "telephone", "mobile", "cell");
        registerSynonymGroup("address", "addr", "location");
        registerSynonymGroup("zip", "zipcode", "zip_code", "postal_code", "postcode");
        registerSynonymGroup("city", "town", "municipality");
        registerSynonymGroup("state", "province", "region");
        registerSynonymGroup("country", "nation", "country_code");
        registerSynonymGroup("street", "street_name", "street_address");
        registerSynonymGroup("created_at", "created_date", "creation_date", "created_time", "create_time");
        registerSynonymGroup("updated_at", "updated_date", "modified_date", "last_modified");
        registerSynonymGroup("status", "state", "condition");
        registerSynonymGroup("active", "is_active", "enabled", "is_enabled");
        registerSynonymGroup("customer_id", "cust_id", "customer_number", "customer_no");
        registerSynonymGroup("user_id", "uid", "user_number");
        registerSynonymGroup("product_id", "item_id", "prod_id", "sku");
        registerSynonymGroup("order_id", "order_number", "order_no");
    }

    private static void registerSynonymGroup(String... terms) {
        Set<String> set = new HashSet<>();
        for (String term : terms) {
            set.add(normalize(term));
        }
        for (String term : set) {
            SYNONYM_MAP.computeIfAbsent(term, k -> new HashSet<>()).addAll(set);
        }
    }

    public ConfidenceScore evaluate(
            String expectedName,
            String candidateName,
            String expectedType,
            String candidateType,
            Map<String, Object> context,
            int historicalSuccesses
    ) {
        if (expectedName == null || candidateName == null) {
            return new ConfidenceScore(0.0, ConfidenceLevel.LOW, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Collections.emptyMap());
        }

        String normExpected = normalize(expectedName);
        String normCandidate = normalize(candidateName);

        // 1. Syntactic name similarity
        double jaroWinkler = jaroWinklerSimilarity(normExpected, normCandidate);
        double levenshtein = normalizedLevenshtein(normExpected, normCandidate);
        double tokenSim = tokenJaccardSimilarity(normExpected, normCandidate);

        // Combined syntactic score
        double nameSim = (jaroWinkler * 0.45) + (levenshtein * 0.35) + (tokenSim * 0.20);
        if (normExpected.equals(normCandidate)) {
            nameSim = 1.0;
        }

        // Exact match when ignoring underscores/casing (e.g. customer_name vs CUSTOMERNAME, userName vs username)
        String flatExp = normExpected.replace("_", "");
        String flatCand = normCandidate.replace("_", "");
        if (!flatExp.isEmpty() && flatExp.equals(flatCand)) {
            nameSim = 1.0;
        }

        // Singular / Plural variation (e.g. customers vs customer)
        if (!flatExp.isEmpty() && !flatCand.isEmpty()) {
            if (flatExp.equals(flatCand + "s") || flatCand.equals(flatExp + "s") ||
                flatExp.equals(flatCand + "es") || flatCand.equals(flatExp + "es")) {
                nameSim = Math.max(nameSim, 0.96);
            }
        }

        // 2. Semantic synonym score
        double semanticSim = evaluateSemanticSimilarity(normExpected, normCandidate);

        // 3. Table-prefixed attribute match (e.g. table "customer", column "customername" or "customer_name" vs expected "name")
        if (context != null && context.containsKey("tableName")) {
            String table = normalize(String.valueOf(context.get("tableName")));
            if (!table.isEmpty()) {
                String tableFlat = table.replace("_", "");
                if (flatCand.equals(tableFlat + flatExp) || flatCand.equals(table + "_" + flatExp) ||
                    (flatExp.equals("name") && flatCand.endsWith("name") && flatCand.startsWith(tableFlat))) {
                    nameSim = Math.max(nameSim, 0.98);
                    semanticSim = Math.max(semanticSim, 1.0);
                }
            }
        }

        // 4. Type compatibility check
        double typeScore = evaluateTypeCompatibility(expectedType, candidateType);

        // 5. Structural / context matching
        double contextScore = evaluateContextMatch(normExpected, normCandidate, context);

        // 6. Relationship check: do expected and candidate share or conflict on entity root?
        double relationshipMultiplier = evaluateRelationshipConstraint(normExpected, normCandidate);

        // 7. Historical weight
        double historyMultiplier = 1.0;
        if (historicalSuccesses > 0) {
            historyMultiplier = Math.min(1.15, 1.0 + (historicalSuccesses * 0.03));
        }

        // Weighted aggregation: if verified synonym, take the maximum rather than diluting
        double baseSemanticOrName = Math.max(nameSim, semanticSim);
        double combinedBase = (baseSemanticOrName * 0.50) + (typeScore * 0.30) + (contextScore * 0.20);

        // Apply relationship and history modifiers
        double overallScore = combinedBase * relationshipMultiplier * historyMultiplier;

        // If types are strictly incompatible, cap score to prevent accidental healing
        if (typeScore <= 0.2) {
            overallScore = Math.min(0.55, overallScore);
        }

        // If relationship check failed completely (different entities e.g. customer_id vs product_id), cap score
        if (relationshipMultiplier < 0.5) {
            overallScore = Math.min(0.40, overallScore);
        }

        overallScore = Math.max(0.0, Math.min(1.0, overallScore));
        ConfidenceLevel level = ConfidenceLevel.fromScore(overallScore);

        Map<String, Object> details = new HashMap<>();
        details.put("expectedName", expectedName);
        details.put("candidateName", candidateName);
        details.put("normExpected", normExpected);
        details.put("normCandidate", normCandidate);
        details.put("nameSim", nameSim);
        details.put("semanticSim", semanticSim);
        details.put("typeScore", typeScore);
        details.put("contextScore", contextScore);
        details.put("relationshipMultiplier", relationshipMultiplier);
        details.put("historyMultiplier", historyMultiplier);

        return new ConfidenceScore(
                overallScore,
                level,
                nameSim,
                semanticSim,
                typeScore,
                contextScore,
                relationshipMultiplier,
                historyMultiplier,
                details
        );
    }

    /**
     * Normalizes names by converting camelCase, kebab-case, snake_case into snake_case and lowercase.
     * E.g. "userName" -> "user_name", "customerId" -> "customer_id", "user-name" -> "user_name"
     */
    public static String normalize(String input) {
        if (input == null) return "";
        // Replace punctuation/dashes with underscore
        String s = input.trim().replaceAll("[-._\\s]+", "_");
        // Separate camelCase
        s = s.replaceAll("(?<=[a-z0-9])(?=[A-Z])", "_").toLowerCase(Locale.ROOT);
        // Clean leading/trailing underscores and multiples
        return s.replaceAll("_+", "_").replaceAll("^_|_$", "");
    }

    private double evaluateSemanticSimilarity(String exp, String cand) {
        if (exp.equals(cand)) return 1.0;
        Set<String> expSyns = SYNONYM_MAP.get(exp);
        if (expSyns != null && expSyns.contains(cand)) {
            return 1.0;
        }

        // Check if one is a substring of the other with high overlap (e.g. "username" vs "name")
        if (exp.contains(cand) || cand.contains(exp)) {
            int minLen = Math.min(exp.length(), cand.length());
            int maxLen = Math.max(exp.length(), cand.length());
            return (double) minLen / maxLen * 0.9;
        }
        return 0.0;
    }

    /**
     * Strict type comparison.
     * Compatible types (Integer <-> Long, String <-> String) get high score.
     * Incompatible types (Integer <-> String, Boolean <-> Array) get low score or penalty.
     */
    public double evaluateTypeCompatibility(String expType, String candType) {
        if (expType == null || candType == null || expType.equalsIgnoreCase("UNKNOWN") || candType.equalsIgnoreCase("UNKNOWN")) {
            return 0.85; // neutral when unknown
        }

        String t1 = expType.trim().toUpperCase(Locale.ROOT);
        String t2 = candType.trim().toUpperCase(Locale.ROOT);

        if (t1.equals(t2)) return 1.0;

        // Group numeric
        boolean t1Num = isNumericType(t1);
        boolean t2Num = isNumericType(t2);
        if (t1Num && t2Num) return 0.95;

        // Group string/varchar/text
        boolean t1Text = isTextType(t1);
        boolean t2Text = isTextType(t2);
        if (t1Text && t2Text) return 1.0;

        // Group boolean/bit
        boolean t1Bool = isBooleanType(t1);
        boolean t2Bool = isBooleanType(t2);
        if (t1Bool && t2Bool) return 1.0;

        // Group date/time
        boolean t1Date = isDateType(t1);
        boolean t2Date = isDateType(t2);
        if (t1Date && t2Date) return 0.95;

        // Group collection/array
        boolean t1List = isListType(t1);
        boolean t2List = isListType(t2);
        if (t1List && t2List) return 0.95;

        // Group object/map/json
        boolean t1Obj = isObjectType(t1);
        boolean t2Obj = isObjectType(t2);
        if (t1Obj && t2Obj) return 0.95;

        // Strict incompatibility: Integer to String or Boolean to Object
        return 0.10;
    }

    private boolean isNumericType(String t) {
        return t.contains("INT") || t.contains("LONG") || t.contains("DECIMAL") ||
                t.contains("DOUBLE") || t.contains("FLOAT") || t.contains("NUMERIC") ||
                t.contains("BIGINT") || t.contains("SMALLINT") || t.equals("NUMBER");
    }

    private boolean isTextType(String t) {
        return t.contains("VARCHAR") || t.contains("STRING") || t.contains("CHAR") ||
                t.contains("TEXT") || t.contains("CLOB");
    }

    private boolean isBooleanType(String t) {
        return t.contains("BOOL") || t.contains("BIT");
    }

    private boolean isDateType(String t) {
        return t.contains("DATE") || t.contains("TIME") || t.contains("TIMESTAMP");
    }

    private boolean isListType(String t) {
        return t.contains("LIST") || t.contains("ARRAY") || t.contains("COLLECTION");
    }

    private boolean isObjectType(String t) {
        return t.contains("OBJECT") || t.contains("MAP") || t.contains("JSON");
    }

    /**
     * Validates relationship and entity root.
     * Prevents mapping "customer_id" to "product_id".
     */
    private double evaluateRelationshipConstraint(String exp, String cand) {
        String expRoot = extractEntityRoot(exp);
        String candRoot = extractEntityRoot(cand);

        if (expRoot != null && candRoot != null && !expRoot.equals(candRoot)) {
            // Distinct entities: severe penalty!
            return 0.20;
        }
        return 1.0;
    }

    private String extractEntityRoot(String term) {
        for (String root : ENTITY_ROOTS) {
            if (term.startsWith(root + "_") || term.startsWith(root) || term.contains("_" + root + "_")) {
                return root;
            }
        }
        return null;
    }

    private double evaluateContextMatch(String exp, String cand, Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return 1.0; // neutral
        }
        double match = 1.0;
        // Table context match
        if (context.containsKey("tableName")) {
            String table = normalize(String.valueOf(context.get("tableName")));
            if (exp.contains(table) || cand.contains(table)) {
                match = 1.0;
            }
        }
        return match;
    }

    // Levenshtein distance normalized to [0, 1]
    public static double normalizedLevenshtein(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }

    // Jaro-Winkler similarity
    public static double jaroWinklerSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 || len2 == 0) return 0.0;

        int matchDistance = Math.max(len1, len2) / 2 - 1;
        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];

        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) return 0.0;

        int transpositions = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) transpositions++;
            k++;
        }

        double jaro = ((double) matches / len1 +
                (double) matches / len2 +
                (double) (matches - transpositions / 2) / matches) / 3.0;

        // Winkler prefix bonus
        int prefix = 0;
        for (int i = 0; i < Math.min(4, Math.min(len1, len2)); i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }

        return jaro + (prefix * 0.1 * (1.0 - jaro));
    }

    // Jaccard similarity across token sets
    private double tokenJaccardSimilarity(String s1, String s2) {
        Set<String> set1 = new HashSet<>(Arrays.asList(s1.split("_")));
        Set<String> set2 = new HashSet<>(Arrays.asList(s2.split("_")));
        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}

