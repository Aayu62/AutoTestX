package com.autotestx.api;

import com.autotestx.constants.Endpoints;
import io.restassured.response.Response;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Order API — wrapper for all order endpoints.
 */
public class OrderAPI extends BaseAPI {

    public Response createOrder(String token, Map<String, Object> payload) {
        log.info("POST {}", Endpoints.ORDERS);
        return given().spec(authSpec(token)).body(payload).post(Endpoints.ORDERS);
    }

    public Response getMyOrders(String token) {
        return given().spec(authSpec(token)).get(Endpoints.ORDERS);
    }

    public Response getMyOrders(String token, int page, int size) {
        return given().spec(authSpec(token))
                .queryParam("page", page)
                .queryParam("size", size)
                .get(Endpoints.ORDERS);
    }

    public Response getOrderById(String token, Long id) {
        log.info("GET {} | id: {}", Endpoints.ORDERS_BY_ID, id);
        return given().spec(authSpec(token)).pathParam("id", id).get(Endpoints.ORDERS_BY_ID);
    }

    public Response cancelOrder(String token, Long id) {
        log.info("DELETE {} | id: {}", Endpoints.ORDERS_CANCEL, id);
        return given().spec(authSpec(token)).pathParam("id", id).delete(Endpoints.ORDERS_CANCEL);
    }

    public Response createOrderWithoutToken(Map<String, Object> payload) {
        return given().spec(publicSpec).body(payload).post(Endpoints.ORDERS);
    }
}
