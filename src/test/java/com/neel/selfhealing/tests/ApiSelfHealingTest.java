package com.neel.selfhealing.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.neel.selfhealing.api.ApiHealingEngine;
import com.neel.selfhealing.api.EndpointHealer;
import com.neel.selfhealing.api.SchemaHealer;
import com.neel.selfhealing.api.SelfHealingJsonPath;
import com.neel.selfhealing.confidence.ConfidenceLevel;
import com.neel.selfhealing.config.SelfHealingConfig;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Verification Suite for API Self-Healing Engine.
 * Implements the 10 mandatory API verification tests specified in Section 29.
 */
public class ApiSelfHealingTest {

    private final ApiHealingEngine apiHealingEngine = ApiHealingEngine.getInstance();

    // 1. Original field exists
    @Test
    public void test1_OriginalFieldExists() {
        String json = "{\"userName\": \"Neel\", \"email\": \"neel@example.com\"}";
        SelfHealingJsonPath jsonPath = SelfHealingJsonPath.from(json, "test1_OriginalFieldExists");

        String userName = jsonPath.getString("userName");
        assertEquals(userName, "Neel", "Original field should be read directly without healing");
    }

    // 2. Field renamed (userName -> username)
    @Test
    public void test2_FieldRenamed() {
        String json = "{\"username\": \"Neel\", \"email\": \"neel@example.com\"}";
        SelfHealingJsonPath jsonPath = SelfHealingJsonPath.from(json, "test2_FieldRenamed");

        String userName = jsonPath.getString("userName");
        assertEquals(userName, "Neel", "Renamed field 'userName' should heal to 'username'");
    }

    // 3. Nested field changed (address.zip -> address.zipcode)
    @Test
    public void test3_NestedFieldChanged() {
        String json = "{\"user\": {\"name\": \"Neel\", \"address\": {\"zipcode\": \"560001\"}}}";
        SelfHealingJsonPath jsonPath = SelfHealingJsonPath.from(json, "test3_NestedFieldChanged");

        String zip = jsonPath.getString("user.address.zip");
        assertEquals(zip, "560001", "Nested field 'zip' should heal to 'zipcode'");
    }

    // 4. JSONPath changed ($.data.user.name -> $.result.user.name)
    @Test
    public void test4_JsonPathChanged() {
        String json = "{\"result\": {\"user\": {\"name\": \"Neel\"}}}";
        SelfHealingJsonPath jsonPath = SelfHealingJsonPath.from(json, "test4_JsonPathChanged");

        String name = jsonPath.getString("$.data.user.name");
        assertEquals(name, "Neel", "JSONPath '$.data.user.name' should heal to candidate path '$.result.user.name'");
    }

    // 5. Schema changed
    @Test
    public void test5_SchemaChanged() {
        String json = "{\"id\": 101, \"username\": \"Neel\", \"active\": true}";
        SchemaHealer schemaHealer = apiHealingEngine.getSchemaHealer();

        Map<String, String> expectedSchema = new HashMap<>();
        expectedSchema.put("userId", "INTEGER");
        expectedSchema.put("userName", "STRING");
        expectedSchema.put("active", "BOOLEAN");

        SchemaHealer.SchemaMappingDiagnostic diagnostic = schemaHealer.analyzeSchema(
                "test5_SchemaChanged",
                expectedSchema,
                json
        );

        assertTrue(diagnostic.isValid(), "Schema should be validly mapped");
        assertEquals(diagnostic.getFieldMappings().get("userId"), "id");
        assertEquals(diagnostic.getFieldMappings().get("userName"), "username");
        assertEquals(diagnostic.getFieldMappings().get("active"), "active");
        assertEquals(diagnostic.getConfidenceLevel(), ConfidenceLevel.HIGH);
    }

    // 6. Multiple possible mappings (disambiguates using semantic & type context)
    @Test
    public void test6_MultiplePossibleMappings() {
        String json = "{\"cust_number\": 555, \"customer_name\": \"Neel\", \"user_phone\": \"123456\"}";
        SelfHealingJsonPath jsonPath = SelfHealingJsonPath.from(json, "test6_MultiplePossibleMappings");

        String name = jsonPath.getString("name");
        assertEquals(name, "Neel", "Should map 'name' to 'customer_name' rather than phone or number");
    }

    // 7. High-confidence mapping
    @Test
    public void test7_HighConfidenceMapping() {
        String json = "{\"customer_id\": 999}";
        SelfHealingJsonPath jsonPath = SelfHealingJsonPath.from(json, "test7_HighConfidenceMapping");

        int customerId = jsonPath.getInt("customerId");
        assertEquals(customerId, 999, "customerId should map to customer_id with HIGH confidence");
    }

    // 8. Low-confidence mapping (fails safely)
    @Test
    public void test8_LowConfidenceMapping() {
        String json = "{\"completely_unrelated_token\": \"abc\"}";
        SelfHealingJsonPath jsonPath = SelfHealingJsonPath.from(json, "test8_LowConfidenceMapping");

        String result = jsonPath.getString("customerId");
        assertNull(result, "Low confidence mapping must be rejected and return null / fail safely");
    }

    // 9. Endpoint version change (/api/v1/users -> /api/v2/users)
    @Test
    public void test9_EndpointVersionChange() {
        EndpointHealer healer = apiHealingEngine.getEndpointHealer();
        String originalEndpoint = "/api/v1/users";
        String healedEndpoint = healer.healEndpoint("test9_EndpointVersionChange", originalEndpoint);

        assertEquals(healedEndpoint, "/api/v2/users", "Endpoint /api/v1/users should heal to /api/v2/users");
    }

    // 10. Healing disabled
    @Test
    public void test10_HealingDisabled() {
        try {
            System.setProperty("API_SELF_HEALING_ENABLED", "false");
            SelfHealingConfig.reload();

            String json = "{\"username\": \"Neel\"}";
            JsonNode result = ApiHealingEngine.getInstance().resolveField(
                    "test10_HealingDisabled",
                    json,
                    "userName",
                    "STRING"
            );

            assertNull(result, "When self-healing is disabled, no healing should occur");
        } finally {
            System.clearProperty("API_SELF_HEALING_ENABLED");
            SelfHealingConfig.reload();
        }
    }
}

