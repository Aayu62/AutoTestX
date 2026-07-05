package com.autotestx.api;

import com.autotestx.constants.Endpoints;
import io.restassured.response.Response;
import static io.restassured.RestAssured.given;
import java.util.Map;
import java.util.HashMap;

public class WishlistAPI extends BaseAPI {

    public Response getWishlist(String token) {
        return given()
                .spec(authSpec(token))
                .when()
                .get(Endpoints.WISHLIST)
                .then()
                .extract().response();
    }

    public Response addToWishlist(String token, Long bookId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookId", bookId);

        return given()
                .spec(authSpec(token))
                .body(payload)
                .when()
                .post(Endpoints.WISHLIST)
                .then()
                .extract().response();
    }

    public Response removeFromWishlist(String token, Long bookId) {
        return given()
                .spec(authSpec(token))
                .pathParam("id", bookId)
                .when()
                .delete(Endpoints.WISHLIST + "/{id}")
                .then()
                .extract().response();
    }
}
