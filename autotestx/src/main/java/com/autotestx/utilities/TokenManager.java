package com.autotestx.utilities;

import com.autotestx.constants.Endpoints;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;

/**
 * Singleton token manager.
 * Stores and manages JWT access tokens and refresh tokens for:
 *   - Regular user
 *   - Admin user
 *
 * Automatically refreshes the access token on a 401 response.
 *
 * Usage:
 *   TokenManager.getInstance().getUserToken()
 *   TokenManager.getInstance().getAdminToken()
 */
public class TokenManager {

    private static final Logger log = LogManager.getLogger(TokenManager.class);
    private static volatile TokenManager instance;

    private final Map<String, String> accessTokens  = new ConcurrentHashMap<>();
    private final Map<String, String> refreshTokens = new ConcurrentHashMap<>();

    private static final String USER_KEY  = "USER";
    private static final String ADMIN_KEY = "ADMIN";

    private TokenManager() {}

    public static TokenManager getInstance() {
        if (instance == null) {
            synchronized (TokenManager.class) {
                if (instance == null) {
                    instance = new TokenManager();
                }
            }
        }
        return instance;
    }

    /**
     * Login as regular user and store tokens.
     */
    public void initUserToken() {
        login(USER_KEY,
              ConfigReader.get("user.email"),
              ConfigReader.get("user.password"));
    }

    /**
     * Login as admin user and store tokens.
     */
    public void initAdminToken() {
        login(ADMIN_KEY,
              ConfigReader.get("admin.email"),
              ConfigReader.get("admin.password"));
    }

    public String getUserToken() {
        return getOrRefresh(USER_KEY);
    }

    public String getAdminToken() {
        return getOrRefresh(ADMIN_KEY);
    }

    public String getUserRefreshToken() {
        return refreshTokens.get(USER_KEY);
    }

    public void setUserToken(String token) {
        accessTokens.put(USER_KEY, token);
    }

    public void setAdminToken(String token) {
        accessTokens.put(ADMIN_KEY, token);
    }

    public void clearAll() {
        accessTokens.clear();
        refreshTokens.clear();
        log.info("All tokens cleared");
    }

    private String getOrRefresh(String role) {
        String token = accessTokens.get(role);
        if (token == null || token.isBlank()) {
            log.warn("{} token missing, attempting refresh", role);
            refreshToken(role);
            token = accessTokens.get(role);
        }
        return token;
    }

    private void login(String role, String email, String password) {
        log.info("Logging in as {} with email: {}", role, email);
        String baseUrl = ConfigReader.get("base.url");

        Response response = given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .body(Map.of("email", email, "password", password))
                .post(Endpoints.AUTH_LOGIN);

        if (response.statusCode() == 200) {
            String accessToken  = response.jsonPath().getString("data.accessToken");
            String refreshToken = response.jsonPath().getString("data.refreshToken");
            accessTokens.put(role, accessToken);
            refreshTokens.put(role, refreshToken);
            log.info("{} login successful", role);
        } else {
            throw new RuntimeException("Login failed for " + role + ": " + response.body().asString());
        }
    }

    private void refreshToken(String role) {
        String rt = refreshTokens.get(role);
        if (rt == null) {
            log.warn("No refresh token available for {}, re-logging in", role);
            if (ADMIN_KEY.equals(role)) initAdminToken();
            else initUserToken();
            return;
        }

        String baseUrl = ConfigReader.get("base.url");
        Response response = given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .body(Map.of("refreshToken", rt))
                .post(Endpoints.AUTH_REFRESH);

        if (response.statusCode() == 200) {
            accessTokens.put(role, response.jsonPath().getString("data.accessToken"));
            log.info("Token refreshed for {}", role);
        } else {
            log.warn("Refresh failed for {}, re-logging in", role);
            if (ADMIN_KEY.equals(role)) initAdminToken();
            else initUserToken();
        }
    }
}
