package com.example.ecommerce.testdata;

import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom assertion helpers for testing.
 * Provides fluent assertions for common test scenarios.
 *
 * Usage:
 * TestAssertions.assertSuccessResponse(mvcResult);
 * TestAssertions.assertErrorResponse(mvcResult, 400, "Bad Request");
 */
public class TestAssertions {

    private static String responseBody(MvcResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Assert HTTP status is OK (200).
     */
    public static void assertOkStatus(MvcResult result) {
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    /**
     * Assert HTTP status is CREATED (201).
     */
    public static void assertCreatedStatus(MvcResult result) {
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
    }

    /**
     * Assert HTTP status is BAD REQUEST (400).
     */
    public static void assertBadRequestStatus(MvcResult result) {
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    /**
     * Assert HTTP status is UNAUTHORIZED (401).
     */
    public static void assertUnauthorizedStatus(MvcResult result) {
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    /**
     * Assert HTTP status is FORBIDDEN (403).
     */
    public static void assertForbiddenStatus(MvcResult result) {
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
    }

    /**
     * Assert HTTP status is NOT FOUND (404).
     */
    public static void assertNotFoundStatus(MvcResult result) {
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    /**
     * Assert HTTP status is INTERNAL SERVER ERROR (500).
     */
    public static void assertServerErrorStatus(MvcResult result) {
        assertThat(result.getResponse().getStatus()).isEqualTo(500);
    }

    /**
     * Assert response status code.
     */
    public static void assertStatusCode(MvcResult result, int expectedStatus) {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
    }

    /**
     * Assert response content type is JSON.
     */
    public static void assertJsonContentType(MvcResult result) {
        assertThat(result.getResponse().getContentType())
                .isNotNull()
                .contains("application/json");
    }

    /**
     * Assert response has content.
     */
    public static void assertHasContent(MvcResult result) {
        assertThat(responseBody(result))
                .isNotNull()
                .isNotEmpty();
    }

    /**
     * Assert response content contains string.
     */
    public static void assertResponseContains(MvcResult result, String content) {
        assertThat(responseBody(result))
                .contains(content);
    }

    /**
     * Assert error response structure.
     */
    public static void assertErrorResponseStructure(MvcResult result, int status) {
        assertStatusCode(result, status);
        assertJsonContentType(result);
        assertHasContent(result);
        assertResponseContains(result, "error");
    }
}
