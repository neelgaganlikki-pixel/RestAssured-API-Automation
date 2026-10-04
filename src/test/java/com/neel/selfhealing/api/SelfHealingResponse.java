package com.neel.selfhealing.api;

import io.restassured.response.ResponseBody;
import io.restassured.response.Response;

/**
 * Production wrapper around REST Assured Response providing self-healing jsonPath.
 */
public class SelfHealingResponse {

    private final Response originalResponse;
    private final String testName;

    public SelfHealingResponse(Response originalResponse, String testName) {
        this.originalResponse = originalResponse;
        this.testName = testName != null ? testName : "ApiTest";
    }

    public static SelfHealingResponse from(Response response, String testName) {
        return new SelfHealingResponse(response, testName);
    }

    public static SelfHealingResponse from(Response response) {
        return new SelfHealingResponse(response, "ApiTest");
    }

    public int getStatusCode() {
        return originalResponse.getStatusCode();
    }

    public ResponseBody<?> getBody() {
        return originalResponse.getBody();
    }

    public String asString() {
        return originalResponse.getBody().asString();
    }

    public String getHeader(String name) {
        return originalResponse.getHeader(name);
    }

    public SelfHealingJsonPath jsonPath() {
        return new SelfHealingJsonPath(originalResponse.getBody().asString(), testName);
    }

    public SelfHealingJsonPath selfHealingJsonPath() {
        return jsonPath();
    }

    public Response getOriginalResponse() {
        return originalResponse;
    }
}

