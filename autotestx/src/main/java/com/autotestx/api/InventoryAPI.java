package com.autotestx.api;

import com.autotestx.constants.Endpoints;
import io.restassured.response.Response;
import static io.restassured.RestAssured.given;
import java.util.Map;
import java.util.HashMap;

public class InventoryAPI extends BaseAPI {

    public Response getInventory(String token, Long bookId) {
        return given()
                .spec(authSpec(token))
                .pathParam("id", bookId)
                .when()
                .get(Endpoints.INVENTORY + "/{id}")
                .then()
                .extract().response();
    }

    public Response updateInventory(String token, Long bookId, int amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amount);

        return given()
                .spec(authSpec(token))
                .pathParam("id", bookId)
                .body(payload)
                .when()
                .put(Endpoints.INVENTORY + "/{id}")
                .then()
                .extract().response();
    }
}
