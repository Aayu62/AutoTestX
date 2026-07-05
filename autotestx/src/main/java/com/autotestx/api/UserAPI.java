package com.autotestx.api;

import com.autotestx.constants.Endpoints;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * User API — wrapper for user/admin endpoints.
 */
public class UserAPI extends BaseAPI {

    public Response getMyProfile(String token) {
        return given().spec(authSpec(token)).get(Endpoints.USERS_ME);
    }

    public Response getUserById(String token, Long id) {
        return given().spec(authSpec(token)).pathParam("id", id).get(Endpoints.USERS_BY_ID);
    }

    public Response updateUser(String token, Long id, java.util.Map<String, Object> payload) {
        return given().spec(authSpec(token)).pathParam("id", id).body(payload).put(Endpoints.USERS_BY_ID);
    }

    public Response deleteUser(String adminToken, Long id) {
        return given().spec(authSpec(adminToken)).pathParam("id", id).delete(Endpoints.USERS_BY_ID);
    }

    public Response getAdminDashboard(String adminToken) {
        return given().spec(authSpec(adminToken)).get(Endpoints.ADMIN_DASHBOARD);
    }

    public Response getAdminDashboardWithoutToken() {
        return given().spec(publicSpec).get(Endpoints.ADMIN_DASHBOARD);
    }
}
