package com.neel.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BasicApiTest {

    private TestExecutionLogger testExecutionLogger;

    // ============================================================
    // TEST SUITE SETUP
    // ============================================================

    @BeforeSuite(alwaysRun = true)
    public void startTestLogging() {

        testExecutionLogger = new TestExecutionLogger();

        testExecutionLogger.startLogging();
    }

    @AfterSuite(alwaysRun = true)
    public void stopTestLogging() {

        if (testExecutionLogger != null) {
            testExecutionLogger.stopLogging();
        }
    }

    // ============================================================
    // TEST 1: GET a single user by ID
    // ============================================================

    @Test
    public void testGetUserById() {

        Response response = RestAssured
                .given()
                .baseUri("https://jsonplaceholder.typicode.com")

                .when()
                .get("/users/1");

        System.out.println();
        System.out.println("=== Test 1: GET User by ID ===");
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body:");
        System.out.println(response.getBody().asString());

        assertEquals(
                response.getStatusCode(),
                200,
                "GET user by ID should return status 200"
        );

        int id = response.jsonPath().getInt("id");

        String name = response.jsonPath().getString("name");

        String email = response.jsonPath().getString("email");

        System.out.println("User ID: " + id);
        System.out.println("User Name: " + name);
        System.out.println("User Email: " + email);

        System.out.println("================================");
        System.out.println();

        assertEquals(
                id,
                1,
                "User ID should be 1"
        );

        assertNotNull(
                name,
                "Name should not be null"
        );

        assertNotNull(
                email,
                "Email should not be null"
        );
    }

    // ============================================================
    // TEST 2: GET all users
    // ============================================================

    @Test
    public void testGetAllUsers() {

        Response response = RestAssured
                .given()
                .baseUri("https://jsonplaceholder.typicode.com")

                .when()
                .get("/users");

        System.out.println();
        System.out.println("=== Test 2: GET All Users ===");
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body:");
        System.out.println(response.getBody().asString());

        assertEquals(
                response.getStatusCode(),
                200,
                "GET all users should return status 200"
        );

        int userCount =
                response.jsonPath()
                        .getList("$")
                        .size();

        System.out.println(
                "Number of users returned: "
                        + userCount
        );

        System.out.println("================================");
        System.out.println();

        assertTrue(
                userCount > 0,
                "Users list should not be empty"
        );
    }

    // ============================================================
    // TEST 3: GET user with path parameter
    // ============================================================

    @Test
    public void testGetUserWithPathParam() {

        int requestedId = 2;

        Response response = RestAssured
                .given()
                .baseUri("https://jsonplaceholder.typicode.com")
                .pathParam("id", requestedId)

                .when()
                .get("/users/{id}");

        System.out.println();
        System.out.println("=== Test 3: GET User with Path Param ===");
        System.out.println("Requested ID: " + requestedId);
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body:");
        System.out.println(response.getBody().asString());

        assertEquals(
                response.getStatusCode(),
                200,
                "GET user with path parameter should return status 200"
        );

        int returnedId =
                response.jsonPath().getInt("id");

        String name =
                response.jsonPath().getString("name");

        System.out.println("Returned ID: " + returnedId);
        System.out.println("User Name: " + name);

        System.out.println("================================");
        System.out.println();

        assertEquals(
                returnedId,
                requestedId,
                "Returned user ID should match requested ID"
        );
    }

    // ============================================================
    // TEST 4: GET users with query parameter
    // ============================================================

    @Test
    public void testGetUsersWithQueryParam() {

        String username = "Bret";

        Response response = RestAssured
                .given()
                .baseUri("https://jsonplaceholder.typicode.com")
                .queryParam("username", username)

                .when()
                .get("/users");

        System.out.println();
        System.out.println("=== Test 4: GET Users with Query Param ===");
        System.out.println("Query: username=" + username);
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body:");
        System.out.println(response.getBody().asString());

        assertEquals(
                response.getStatusCode(),
                200,
                "GET users with query parameter should return status 200"
        );

        int resultCount =
                response.jsonPath()
                        .getList("$")
                        .size();

        System.out.println(
                "Results found: "
                        + resultCount
        );

        System.out.println("================================");
        System.out.println();

        assertTrue(
                resultCount > 0,
                "Should find at least one user with username 'Bret'"
        );
    }

    // ============================================================
    // TEST 5: POST a new user
    // ============================================================

    @Test
    public void testPostNewUser() {

        String requestBody =
                "{\n" +
                "  \"name\": \"Neel\",\n" +
                "  \"username\": \"neel\",\n" +
                "  \"email\": \"neel@example.com\"\n" +
                "}";

        System.out.println();
        System.out.println("=== Test 5: POST New User ===");
        System.out.println("Request Body:");
        System.out.println(requestBody);

        /*
         * Capture the response BEFORE performing the assertion.
         * This allows us to see the actual status code and response
         * body in Jenkins when the API returns an unexpected status.
         */
        Response response = RestAssured
                .given()
                .baseUri("https://jsonplaceholder.typicode.com")
                .contentType("application/json")
                .body(requestBody)

                .when()
                .post("/users");

        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body:");
        System.out.println(response.getBody().asString());

        /*
         * Expected response from JSONPlaceholder:
         * HTTP 201 Created
         */
        assertEquals(
                response.getStatusCode(),
                201,
                "POST new user should return status 201"
        );

        String returnedName =
                response.jsonPath()
                        .getString("name");

        System.out.println("Returned Name: " + returnedName);

        System.out.println("================================");
        System.out.println();

        assertEquals(
                returnedName,
                "Neel",
                "Returned name should be 'Neel'"
        );
    }

    // ============================================================
    // TEST 6: PUT update an existing user
    // ============================================================

    @Test
    public void testPutUpdateUser() {

        String requestBody =
                "{\n" +
                "  \"id\": 1,\n" +
                "  \"name\": \"Updated Neel\",\n" +
                "  \"username\": \"neelupdated\",\n" +
                "  \"email\": \"updated@example.com\"\n" +
                "}";

        System.out.println();
        System.out.println("=== Test 6: PUT Update User ===");
        System.out.println("Request Body:");
        System.out.println(requestBody);

        /*
         * Capture the response BEFORE assertion so Jenkins shows
         * the actual API response when something goes wrong.
         */
        Response response = RestAssured
                .given()
                .baseUri("https://jsonplaceholder.typicode.com")
                .contentType("application/json")
                .body(requestBody)

                .when()
                .put("/users/1");

        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body:");
        System.out.println(response.getBody().asString());

        /*
         * Expected response from JSONPlaceholder:
         * HTTP 200 OK
         */
        assertEquals(
                response.getStatusCode(),
                200,
                "PUT update user should return status 200"
        );

        String updatedName =
                response.jsonPath()
                        .getString("name");

        System.out.println("Updated Name: " + updatedName);

        System.out.println("================================");
        System.out.println();

        assertEquals(
                updatedName,
                "Updated Neel",
                "Name should be updated to 'Updated Neel'"
        );
    }

    // ============================================================
    // TEST 7: DELETE a user
    // ============================================================

    @Test
    public void testDeleteUser() {

        Response response = RestAssured
                .given()
                .baseUri("https://jsonplaceholder.typicode.com")

                .when()
                .delete("/users/1");

        System.out.println();
        System.out.println("=== Test 7: DELETE User ===");
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body:");
        System.out.println(response.getBody().asString());

        assertEquals(
                response.getStatusCode(),
                200,
                "DELETE user should return status 200"
        );

        System.out.println("================================");
        System.out.println();
    }
}

