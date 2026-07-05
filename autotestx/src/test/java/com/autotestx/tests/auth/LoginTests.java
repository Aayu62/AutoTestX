package com.autotestx.tests.auth;

import com.autotestx.api.AuthAPI;
import com.autotestx.assertions.APIAssert;
import com.autotestx.base.BaseTest;
import com.autotestx.utilities.ConfigReader;
import com.autotestx.utilities.ExcelReader;
import com.autotestx.utilities.FakerUtils;
import com.autotestx.utilities.ReportManager;
import com.autotestx.utilities.TokenManager;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.*;
import java.util.Map;

/**
 * Login Test Suite — 15 test cases + Excel DDT
 * Covers: valid login, invalid credentials, locked account, schema, performance
 */
@Feature("Authentication")
public class LoginTests extends BaseTest {

    private final AuthAPI authAPI = new AuthAPI();
    private String validEmail;
    private String validPassword;

    @BeforeClass
    public void setup() {
        validEmail    = ConfigReader.get("user.email");
        validPassword = ConfigReader.get("user.password");
    }

    @Test(priority = 1,
          description = "TC-LOGIN-001: Login with valid credentials should return 200 + tokens")
    @Severity(SeverityLevel.BLOCKER)
    public void loginWithValidCredentials_shouldReturn200() {
        ReportManager.createTest("TC-LOGIN-001 Valid Login");

        Response response = authAPI.login(validEmail, validPassword);

        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyAuthResponse(response);
        APIAssert.verifyField(response, "success", true);
        APIAssert.verifyResponseTime(response, 2000);
    }

    @Test(priority = 2,
          description = "TC-LOGIN-002: Login with wrong password should return 401")
    @Severity(SeverityLevel.CRITICAL)
    public void loginWithWrongPassword_shouldReturn401() {
        ReportManager.createTest("TC-LOGIN-002 Wrong Password");

        Response response = authAPI.login(validEmail, "WrongPass@999");

        APIAssert.verifyUnauthorized(response);
        APIAssert.verifySuccessFalse(response);
    }

    @Test(priority = 3,
          description = "TC-LOGIN-003: Login with non-existent email should return 401")
    @Severity(SeverityLevel.CRITICAL)
    public void loginWithNonExistentEmail_shouldReturn401() {
        ReportManager.createTest("TC-LOGIN-003 Non-existent Email");

        Response response = authAPI.login("nouser_" + FakerUtils.randomEmail(), "Test@1234");

        APIAssert.verifyUnauthorized(response);
    }

    @Test(priority = 4,
          description = "TC-LOGIN-004: Login with blank email should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void loginWithBlankEmail_shouldReturn400() {
        ReportManager.createTest("TC-LOGIN-004 Blank Email");

        Response response = authAPI.login(Map.of("email", "", "password", "Test@1234"));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 5,
          description = "TC-LOGIN-005: Login with blank password should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void loginWithBlankPassword_shouldReturn400() {
        ReportManager.createTest("TC-LOGIN-005 Blank Password");

        Response response = authAPI.login(Map.of("email", validEmail, "password", ""));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 6,
          description = "TC-LOGIN-006: Login response should contain Bearer token type")
    @Severity(SeverityLevel.NORMAL)
    public void loginResponse_shouldContainBearerTokenType() {
        ReportManager.createTest("TC-LOGIN-006 Bearer Token Type");

        Response response = authAPI.login(validEmail, validPassword);
        APIAssert.verifyField(response, "data.tokenType", "Bearer");
    }

    @Test(priority = 7,
          description = "TC-LOGIN-007: Login response should match JSON schema")
    @Severity(SeverityLevel.NORMAL)
    public void loginResponse_shouldMatchSchema() {
        ReportManager.createTest("TC-LOGIN-007 Schema Validation");

        Response response = authAPI.login(validEmail, validPassword);
        APIAssert.verifyStatus(response, 200);
        APIAssert.verifySchema(response, "login-response-schema.json");
    }

    @Test(priority = 8,
          description = "TC-LOGIN-008: Login with SQL injection in password should not cause 500")
    @Severity(SeverityLevel.CRITICAL)
    public void loginWithSqlInjection_shouldNotCause500() {
        ReportManager.createTest("TC-LOGIN-008 SQL Injection Protection");

        Response response = authAPI.login(validEmail, "' OR '1'='1"); 
        // Should be 401 (wrong password), not 500
        APIAssert.verifyStatusIn(response, 400, 401);
    }

    @Test(priority = 9,
          description = "TC-LOGIN-009: Login response time should be under 500ms")
    @Severity(SeverityLevel.NORMAL)
    public void loginResponseTime_shouldBeUnder500ms() {
        ReportManager.createTest("TC-LOGIN-009 Performance - Response Time");

        Response response = authAPI.login(validEmail, validPassword);
        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyResponseTime(response, 500);
    }

    @Test(priority = 10,
          description = "TC-LOGIN-010: Access token should be non-null and non-blank")
    @Severity(SeverityLevel.BLOCKER)
    public void loginAccessToken_shouldBeNonNull() {
        ReportManager.createTest("TC-LOGIN-010 Access Token Present");

        Response response = authAPI.login(validEmail, validPassword);
        String token = response.jsonPath().getString("data.accessToken");
        Assert.assertNotNull(token, "Access token should not be null");
        Assert.assertFalse(token.isBlank(), "Access token should not be blank");
        Assert.assertTrue(token.startsWith("ey"), "JWT should start with 'ey'");
    }

    @Test(priority = 11,
          description = "TC-LOGIN-011: Login with XSS payload in email should not cause 500")
    @Severity(SeverityLevel.CRITICAL)
    public void loginWithXssPayload_shouldNotCause500() {
        ReportManager.createTest("TC-LOGIN-011 XSS Protection");

        Response response = authAPI.login(
            Map.of("email", "<script>alert(1)</script>", "password", "Test@1234"));
        APIAssert.verifyStatusIn(response, 400, 401);
    }

    @Test(priority = 12,
          description = "TC-LOGIN-012: Login with null email field should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void loginWithNullFields_shouldReturn400() {
        ReportManager.createTest("TC-LOGIN-012 Null Fields");

        Response response = authAPI.login(Map.of("password", "Test@1234")); // no email key
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 13,
          description = "TC-LOGIN-013: Admin login should include ADMIN role in response")
    @Severity(SeverityLevel.BLOCKER)
    public void adminLogin_shouldReturnAdminRole() {
        ReportManager.createTest("TC-LOGIN-013 Admin Role in Response");

        Response response = authAPI.login(
            ConfigReader.get("admin.email"),
            ConfigReader.get("admin.password")
        );
        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyField(response, "data.user.role", "ADMIN");
    }

    @Test(priority = 14,
          description = "TC-LOGIN-014: expiresIn value should be positive")
    @Severity(SeverityLevel.NORMAL)
    public void loginExpiresIn_shouldBePositive() {
        ReportManager.createTest("TC-LOGIN-014 ExpiresIn Positive");

        Response response = authAPI.login(validEmail, validPassword);
        Long expiresIn = response.jsonPath().getLong("data.expiresIn");
        Assert.assertTrue(expiresIn != null && expiresIn > 0,
            "expiresIn should be a positive value. Actual: " + expiresIn);
    }

    // ── Data-Driven from Excel ────────────────────────────────────────────────────

    @DataProvider(name = "loginDataFromExcel")
    public Object[][] loginDataFromExcel() {
        return ExcelReader.readSheet("login_test_data.xlsx", "LoginTests");
    }

    @Test(dataProvider = "loginDataFromExcel",
          priority = 15,
          description = "TC-LOGIN-015: Data-driven login tests from Excel")
    @Severity(SeverityLevel.NORMAL)
    public void loginDataDriven(String email, String password, int expectedStatus, String scenario) {
        ReportManager.createTest("TC-LOGIN-015 DDT [Excel]: " + scenario);

        Response response = authAPI.login(Map.of("email", email, "password", password));
        APIAssert.verifyStatus(response, expectedStatus);
    }
}
