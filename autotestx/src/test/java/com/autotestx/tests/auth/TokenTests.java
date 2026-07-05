package com.autotestx.tests.auth;

import com.autotestx.api.AuthAPI;
import com.autotestx.assertions.APIAssert;
import com.autotestx.base.BaseTest;
import com.autotestx.utilities.ConfigReader;
import com.autotestx.utilities.FakerUtils;
import com.autotestx.utilities.ReportManager;
import com.autotestx.utilities.TokenManager;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.*;
import java.util.Map;

/**
 * Token Tests — refresh token and logout test suite
 * 8 test cases
 */
@Feature("Authentication")
public class TokenTests extends BaseTest {

    private final AuthAPI authAPI = new AuthAPI();
    private String userRefreshToken;

    @BeforeClass
    public void setup() {
        TokenManager tm = TokenManager.getInstance();
        tm.initUserToken();
        userRefreshToken = tm.getUserRefreshToken();
    }

    @Test(priority = 1,
          description = "TC-TOKEN-001: Refresh token should return new access token")
    @Severity(SeverityLevel.BLOCKER)
    public void refreshToken_shouldReturnNewAccessToken() {
        ReportManager.createTest("TC-TOKEN-001 Refresh Token Valid");

        Response response = authAPI.refreshToken(userRefreshToken);

        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyFieldNotNull(response, "data.accessToken");
        APIAssert.verifyField(response, "data.tokenType", "Bearer");
    }

    @Test(priority = 2,
          description = "TC-TOKEN-002: Refresh with invalid token should return 401")
    @Severity(SeverityLevel.CRITICAL)
    public void refreshWithInvalidToken_shouldReturn401() {
        ReportManager.createTest("TC-TOKEN-002 Invalid Refresh Token");

        Response response = authAPI.refreshToken("completely-invalid-token-" + FakerUtils.randomString(20));
        APIAssert.verifyUnauthorized(response);
    }

    @Test(priority = 3,
          description = "TC-TOKEN-003: Refresh with blank token should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void refreshWithBlankToken_shouldReturn400() {
        ReportManager.createTest("TC-TOKEN-003 Blank Refresh Token");

        Response response = authAPI.refreshToken("");
        APIAssert.verifyStatusIn(response, 400, 401);
    }

    @Test(priority = 4,
          description = "TC-TOKEN-004: Logout should return 200")
    @Severity(SeverityLevel.CRITICAL)
    public void logout_shouldReturn200() {
        ReportManager.createTest("TC-TOKEN-004 Logout");

        // Login fresh for logout test
        Response loginResp = authAPI.login(
            ConfigReader.get("admin.email"),
            ConfigReader.get("admin.password")
        );
        String rt = loginResp.jsonPath().getString("data.refreshToken");

        Response logoutResp = authAPI.logout(rt);
        APIAssert.verifyStatus(logoutResp, 200);
        APIAssert.verifySuccessTrue(logoutResp);
    }

    @Test(priority = 5,
          description = "TC-TOKEN-005: Logout with invalid refresh token should not fail with 500")
    @Severity(SeverityLevel.NORMAL)
    public void logoutWithInvalidToken_shouldNotReturn500() {
        ReportManager.createTest("TC-TOKEN-005 Logout Invalid Token Graceful");

        Response response = authAPI.logout("fake-refresh-token");
        // Should gracefully handle unknown token — either 200 (idempotent) or 200
        APIAssert.verifyStatusIn(response, 200, 400, 401);
    }

    @Test(priority = 6,
          description = "TC-TOKEN-006: Access with no token should return 401")
    @Severity(SeverityLevel.BLOCKER)
    public void accessProtectedEndpointWithNoToken_shouldReturn401() {
        ReportManager.createTest("TC-TOKEN-006 No Token Access Denied");

        Response response = authAPI.loginWithNoToken();
        APIAssert.verifyUnauthorized(response);
    }

    @Test(priority = 7,
          description = "TC-TOKEN-007: Access with invalid JWT should return 401")
    @Severity(SeverityLevel.BLOCKER)
    public void accessWithInvalidJWT_shouldReturn401() {
        ReportManager.createTest("TC-TOKEN-007 Invalid JWT");

        Response response = authAPI.loginWithInvalidToken();
        APIAssert.verifyUnauthorized(response);
    }

    @Test(priority = 8,
          description = "TC-TOKEN-008: Access with malformed Bearer header should return 401")
    @Severity(SeverityLevel.NORMAL)
    public void accessWithMalformedBearer_shouldReturn401() {
        ReportManager.createTest("TC-TOKEN-008 Malformed Bearer Header");

        Response response = authAPI.loginWithExpiredToken("not.a.real.jwt.at.all");
        APIAssert.verifyUnauthorized(response);
    }
}
