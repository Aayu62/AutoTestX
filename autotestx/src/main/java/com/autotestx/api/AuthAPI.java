package com.autotestx.api;

import com.autotestx.constants.Endpoints;
import io.restassured.response.Response;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Auth API — wrapper for all authentication endpoints.
 *
 * POST /api/auth/register
 * POST /api/auth/login
 * POST /api/auth/logout
 * POST /api/auth/refresh-token
 */
public class AuthAPI extends BaseAPI {

    public Response register(Map<String, Object> payload) {
        log.info("POST {} | email: {}", Endpoints.AUTH_REGISTER, payload.get("email"));
        return given()
                .spec(publicSpec)
                .body(payload)
                .post(Endpoints.AUTH_REGISTER);
    }

    public Response login(String email, String password) {
        log.info("POST {} | email: {}", Endpoints.AUTH_LOGIN, email);
        return given()
                .spec(publicSpec)
                .body(Map.of("email", email, "password", password))
                .post(Endpoints.AUTH_LOGIN);
    }

    public Response login(Map<String, Object> payload) {
        log.info("POST {} | payload: {}", Endpoints.AUTH_LOGIN, payload);
        return given()
                .spec(publicSpec)
                .body(payload)
                .post(Endpoints.AUTH_LOGIN);
    }

    public Response logout(String refreshToken) {
        log.info("POST {}", Endpoints.AUTH_LOGOUT);
        return given()
                .spec(publicSpec)
                .body(Map.of("refreshToken", refreshToken))
                .post(Endpoints.AUTH_LOGOUT);
    }

    public Response refreshToken(String refreshToken) {
        log.info("POST {}", Endpoints.AUTH_REFRESH);
        return given()
                .spec(publicSpec)
                .body(Map.of("refreshToken", refreshToken))
                .post(Endpoints.AUTH_REFRESH);
    }

    public Response loginWithInvalidToken() {
        return given()
                .spec(publicSpec)
                .header("Authorization", "Bearer invalid.jwt.token")
                .get(Endpoints.USERS_ME);
    }

    public Response loginWithNoToken() {
        return given()
                .spec(publicSpec)
                .get(Endpoints.USERS_ME);
    }

    public Response loginWithExpiredToken(String expiredToken) {
        return given()
                .spec(publicSpec)
                .header("Authorization", "Bearer " + expiredToken)
                .get(Endpoints.USERS_ME);
    }
}
