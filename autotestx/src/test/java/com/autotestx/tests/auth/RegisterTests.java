package com.autotestx.tests.auth;

import com.autotestx.api.AuthAPI;
import com.autotestx.assertions.APIAssert;
import com.autotestx.base.BaseTest;
import com.autotestx.listeners.RetryAnalyzer;
import com.autotestx.utilities.DBUtils;
import com.autotestx.utilities.ExcelReader;
import com.autotestx.utilities.FakerUtils;
import com.autotestx.utilities.ReportManager;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.*;
import java.util.Map;

/**
 * Registration Test Suite — 12 test cases
 * Covers: happy path, duplicate email, validation errors, boundary conditions
 */
@Feature("Authentication")
public class RegisterTests extends BaseTest {

    private final AuthAPI authAPI = new AuthAPI();

    @Test(priority = 1,
          description = "TC-AUTH-001: Register with valid credentials should return 201 + tokens")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Register a new user with unique email. Verify 201 response, token presence, and DB record.")
    public void registerWithValidData_shouldReturn201() {
        ReportManager.createTest("TC-AUTH-001 Register Valid User");

        String email = FakerUtils.randomEmail();
        String name  = FakerUtils.randomName();

        Response response = authAPI.register(Map.of(
            "name",     name,
            "email",    email,
            "password", "Test@1234"
        ));

        APIAssert.verifyStatus(response, 201);
        APIAssert.verifyAuthResponse(response);
        APIAssert.verifyField(response, "data.user.email", email);
        APIAssert.verifyField(response, "data.user.role", "USER");
        APIAssert.verifyResponseTime(response, 2000);

        // Database validation
        Assert.assertTrue(DBUtils.getInstance().verifyUserExists(email),
            "User should be persisted in PostgreSQL after registration");
        ReportManager.getTest().pass("DB confirmed: user exists in PostgreSQL");
    }

    @Test(priority = 2,
          description = "TC-AUTH-002: Register with duplicate email should return 409 Conflict")
    @Severity(SeverityLevel.CRITICAL)
    public void registerWithDuplicateEmail_shouldReturn409() {
        ReportManager.createTest("TC-AUTH-002 Duplicate Email Registration");

        String email = FakerUtils.randomEmail();
        // First registration
        authAPI.register(Map.of("name", "First", "email", email, "password", "Test@1234"));

        // Second registration with same email
        Response response = authAPI.register(Map.of(
            "name",     "Second",
            "email",    email,
            "password", "Test@1234"
        ));

        APIAssert.verifyConflict(response);
        APIAssert.verifySuccessFalse(response);
    }

    @Test(priority = 3,
          description = "TC-AUTH-003: Register with blank email should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void registerWithBlankEmail_shouldReturn400() {
        ReportManager.createTest("TC-AUTH-003 Blank Email Validation");

        Response response = authAPI.register(Map.of(
            "name",     FakerUtils.randomName(),
            "email",    "",
            "password", "Test@1234"
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 4,
          description = "TC-AUTH-004: Register with invalid email format should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void registerWithInvalidEmail_shouldReturn400() {
        ReportManager.createTest("TC-AUTH-004 Invalid Email Format");

        Response response = authAPI.register(Map.of(
            "name",     FakerUtils.randomName(),
            "email",    "not-a-valid-email",
            "password", "Test@1234"
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 5,
          description = "TC-AUTH-005: Register with short password should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void registerWithShortPassword_shouldReturn400() {
        ReportManager.createTest("TC-AUTH-005 Short Password Validation");

        Response response = authAPI.register(Map.of(
            "name",     FakerUtils.randomName(),
            "email",    FakerUtils.randomEmail(),
            "password", "Ab1"
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 6,
          description = "TC-AUTH-006: Register with missing name should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void registerWithMissingName_shouldReturn400() {
        ReportManager.createTest("TC-AUTH-006 Missing Name Field");

        Response response = authAPI.register(Map.of(
            "email",    FakerUtils.randomEmail(),
            "password", "Test@1234"
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 7,
          description = "TC-AUTH-007: Register with empty body should return 400")
    @Severity(SeverityLevel.MINOR)
    public void registerWithEmptyBody_shouldReturn400() {
        ReportManager.createTest("TC-AUTH-007 Empty Request Body");

        Response response = authAPI.register(Map.of());
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 8,
          description = "TC-AUTH-008: Register with extremely long name (boundary test)")
    @Severity(SeverityLevel.MINOR)
    public void registerWithLongName_shouldReturn400() {
        ReportManager.createTest("TC-AUTH-008 Boundary - Long Name");

        String longName = "A".repeat(256);
        Response response = authAPI.register(Map.of(
            "name",     longName,
            "email",    FakerUtils.randomEmail(),
            "password", "Test@1234"
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 9,
          description = "TC-AUTH-009: Register with password missing uppercase should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void registerWithPasswordNoUppercase_shouldReturn400() {
        ReportManager.createTest("TC-AUTH-009 Password No Uppercase");

        Response response = authAPI.register(Map.of(
            "name",     FakerUtils.randomName(),
            "email",    FakerUtils.randomEmail(),
            "password", "test@1234"  // no uppercase
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 10,
          description = "TC-AUTH-010: Register response should match JSON schema")
    @Severity(SeverityLevel.NORMAL)
    public void registerResponse_shouldMatchSchema() {
        ReportManager.createTest("TC-AUTH-010 Schema Validation");

        Response response = authAPI.register(Map.of(
            "name",     FakerUtils.randomName(),
            "email",    FakerUtils.randomEmail(),
            "password", "Test@1234"
        ));
        APIAssert.verifyStatus(response, 201);
        APIAssert.verifySchema(response, "login-response-schema.json");
    }

    @Test(priority = 11,
          description = "TC-AUTH-011: Register with SQL injection in email should return 400 (not 500)")
    @Severity(SeverityLevel.CRITICAL)
    public void registerWithSqlInjectionEmail_shouldReturn400() {
        ReportManager.createTest("TC-AUTH-011 SQL Injection in Email");

        Response response = authAPI.register(Map.of(
            "name",     "Test",
            "email",    "' OR '1'='1'; --",
            "password", "Test@1234"
        ));
        APIAssert.verifyStatusIn(response, 400, 422);
    }

    @Test(priority = 12,
          description = "TC-AUTH-012: Registered user ID should be positive integer")
    @Severity(SeverityLevel.NORMAL)
    public void registeredUser_shouldHavePositiveId() {
        ReportManager.createTest("TC-AUTH-012 User ID Positive Integer");

        Response response = authAPI.register(Map.of(
            "name",     FakerUtils.randomName(),
            "email",    FakerUtils.randomEmail(),
            "password", "Test@1234"
        ));
        APIAssert.verifyStatus(response, 201);
        Integer id = response.jsonPath().getInt("data.user.id");
        Assert.assertTrue(id > 0, "User ID should be a positive integer. Actual: " + id);
    }

    // ── Data-Driven Test ─────────────────────────────────────────────────────────

    @DataProvider(name = "invalidRegistrations")
    public Object[][] invalidRegistrations() {
        return new Object[][] {
            { "", "Test@1234", "Empty name" },
            { FakerUtils.randomName(), "bademail", "Invalid email format" },
            { FakerUtils.randomName(), FakerUtils.randomEmail(), "short" },  // short password
            { FakerUtils.randomName(), FakerUtils.randomEmail(), "nouppercase1" },  // no uppercase
        };
    }

    @Test(dataProvider = "invalidRegistrations",
          priority = 13,
          description = "TC-AUTH-013: Data-driven invalid registration tests")
    @Severity(SeverityLevel.NORMAL)
    public void registerInvalidData_shouldReturn400(String name, String password, String scenario) {
        ReportManager.createTest("TC-AUTH-013 DDT Invalid Registration: " + scenario);

        Response response = authAPI.register(Map.of(
            "name",     name,
            "email",    FakerUtils.randomEmail(),
            "password", password
        ));
        APIAssert.verifyBadRequest(response);
    }
}
