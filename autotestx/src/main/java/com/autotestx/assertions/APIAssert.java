package com.autotestx.assertions;

import com.autotestx.utilities.ReportManager;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.io.InputStream;

/**
 * Custom assertion DSL for AutoTestX.
 *
 * Instead of scattered:
 *   Assert.assertEquals(response.statusCode(), 200)
 *   Assert.assertNotNull(response.jsonPath().getString("data.id"))
 *
 * Use expressive, logged assertions:
 *   APIAssert.verifyStatus(response, 200)
 *   APIAssert.verifyResponseTime(response, 500)
 *   APIAssert.verifySchema(response, "book-schema.json")
 *   APIAssert.verifyField(response, "data.success", true)
 *   APIAssert.verifyBookCreated(response)
 *   APIAssert.verifyOrderStatus(response, "PENDING")
 */
public final class APIAssert {

    private static final Logger log = LogManager.getLogger(APIAssert.class);

    private APIAssert() {}

    // ── Status Code ──────────────────────────────────────────────────────────────

    public static void verifyStatus(Response response, int expectedStatus) {
        int actualStatus = response.statusCode();
        String msg = String.format("Status code | Expected: %d | Actual: %d", expectedStatus, actualStatus);
        if (actualStatus == expectedStatus) {
            log.info("✅ {}", msg);
            logPass(msg);
        } else {
            log.error("❌ {} | Body: {}", msg, response.body().asPrettyString());
            logFail(msg + " | Response: " + response.body().asString());
            Assert.fail(msg);
        }
    }

    public static void verifyStatusIn(Response response, int... expectedStatuses) {
        int actual = response.statusCode();
        for (int exp : expectedStatuses) {
            if (actual == exp) {
                String msg = "Status code " + actual + " is in expected set";
                log.info("✅ {}", msg);
                logPass(msg);
                return;
            }
        }
        String msg = "Status " + actual + " not in expected set: " + java.util.Arrays.toString(expectedStatuses);
        log.error("❌ {}", msg);
        logFail(msg);
        Assert.fail(msg);
    }

    // ── Response Time ────────────────────────────────────────────────────────────

    public static void verifyResponseTime(Response response) {
        verifyResponseTime(response, 500);
    }

    public static void verifyResponseTime(Response response, long maxMs) {
        long actual = response.time();
        String msg = String.format("Response time | Expected: <%dms | Actual: %dms", maxMs, actual);
        if (actual <= maxMs) {
            log.info("✅ {}", msg);
            logPass(msg);
        } else {
            log.warn("⚠️  {}", msg);
            logFail(msg);
            Assert.fail(msg);
        }
    }

    // ── JSON Field Assertions ────────────────────────────────────────────────────

    public static void verifyField(Response response, String jsonPath, Object expectedValue) {
        Object actual = response.jsonPath().get(jsonPath);
        String msg = String.format("Field '%s' | Expected: %s | Actual: %s", jsonPath, expectedValue, actual);
        if (String.valueOf(expectedValue).equals(String.valueOf(actual))) {
            log.info("✅ {}", msg);
            logPass(msg);
        } else {
            log.error("❌ {}", msg);
            logFail(msg);
            Assert.fail(msg);
        }
    }

    public static void verifyFieldNotNull(Response response, String jsonPath) {
        Object value = response.jsonPath().get(jsonPath);
        String msg = String.format("Field '%s' should not be null | Actual: %s", jsonPath, value);
        if (value != null) {
            log.info("✅ {}", msg);
            logPass(msg);
        } else {
            log.error("❌ {}", msg);
            logFail(msg);
            Assert.fail(msg);
        }
    }

    public static void verifyFieldContains(Response response, String jsonPath, String substring) {
        String actual = response.jsonPath().getString(jsonPath);
        String msg = String.format("Field '%s' contains '%s' | Actual: %s", jsonPath, substring, actual);
        if (actual != null && actual.contains(substring)) {
            log.info("✅ {}", msg);
            logPass(msg);
        } else {
            log.error("❌ {}", msg);
            logFail(msg);
            Assert.fail(msg);
        }
    }

    // ── JSON Schema Validation ────────────────────────────────────────────────────

    public static void verifySchema(Response response, String schemaFileName) {
        String schemaPath = "schemas/" + schemaFileName;
        String msg = "JSON Schema validation against: " + schemaFileName;
        try {
            InputStream schema = APIAssert.class.getClassLoader().getResourceAsStream(schemaPath);
            if (schema == null) {
                throw new RuntimeException("Schema file not found: " + schemaPath);
            }
            response.then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(schema));
            log.info("✅ {}", msg);
            logPass(msg);
        } catch (AssertionError e) {
            log.error("❌ Schema validation failed: {}", e.getMessage());
            logFail("Schema validation failed: " + e.getMessage());
            throw e;
        }
    }

    // ── Domain-specific Assertions ────────────────────────────────────────────────

    public static void verifySuccessTrue(Response response) {
        verifyField(response, "success", true);
    }

    public static void verifySuccessFalse(Response response) {
        verifyField(response, "success", false);
    }

    public static void verifyBookCreated(Response response) {
        verifyStatus(response, 201);
        verifyFieldNotNull(response, "data.id");
        verifyFieldNotNull(response, "data.title");
        verifyFieldNotNull(response, "data.isbn");
        verifyFieldNotNull(response, "data.price");
        logPass("Book creation verified — id, title, isbn, price present");
    }

    public static void verifyOrderCreated(Response response) {
        verifyStatus(response, 201);
        verifyFieldNotNull(response, "data.id");
        verifyField(response, "data.status", "PENDING");
        verifyFieldNotNull(response, "data.totalPrice");
        logPass("Order creation verified — id, status=PENDING, totalPrice present");
    }

    public static void verifyOrderStatus(Response response, String expectedStatus) {
        verifyFieldNotNull(response, "data.id");
        verifyField(response, "data.status", expectedStatus);
        logPass("Order status verified: " + expectedStatus);
    }

    public static void verifyAuthResponse(Response response) {
        verifyStatus(response, 200);
        verifyFieldNotNull(response, "data.accessToken");
        verifyFieldNotNull(response, "data.refreshToken");
        verifyField(response, "data.tokenType", "Bearer");
        verifyFieldNotNull(response, "data.user.id");
        verifyFieldNotNull(response, "data.user.email");
        logPass("Auth response verified — tokens, tokenType, user info present");
    }

    public static void verifyPaginatedResponse(Response response) {
        verifyStatus(response, 200);
        verifyFieldNotNull(response, "data.content");
        verifyFieldNotNull(response, "data.totalElements");
        verifyFieldNotNull(response, "data.totalPages");
        verifyFieldNotNull(response, "data.number");
        logPass("Paginated response structure verified");
    }

    public static void verifyUnauthorized(Response response) {
        verifyStatus(response, 401);
        logPass("Unauthorized response verified (401)");
    }

    public static void verifyForbidden(Response response) {
        verifyStatus(response, 403);
        logPass("Forbidden response verified (403)");
    }

    public static void verifyNotFound(Response response) {
        verifyStatus(response, 404);
        logPass("Not found response verified (404)");
    }

    public static void verifyConflict(Response response) {
        verifyStatus(response, 409);
        logPass("Conflict response verified (409)");
    }

    public static void verifyBadRequest(Response response) {
        verifyStatus(response, 400);
        logPass("Bad request response verified (400)");
    }

    // ── Performance Assertion ─────────────────────────────────────────────────────

    public static void verifyPerformance(Response response, int status, long maxMs) {
        verifyStatus(response, status);
        verifyResponseTime(response, maxMs);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private static void logPass(String message) {
        try {
            if (ReportManager.getTest() != null) {
                ReportManager.getTest().pass(message);
            }
        } catch (Exception ignored) {}
    }

    private static void logFail(String message) {
        try {
            if (ReportManager.getTest() != null) {
                ReportManager.getTest().fail(message);
            }
        } catch (Exception ignored) {}
    }
}
