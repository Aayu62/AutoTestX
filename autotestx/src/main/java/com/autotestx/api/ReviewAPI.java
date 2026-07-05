package com.autotestx.api;

import com.autotestx.constants.Endpoints;
import io.restassured.response.Response;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Review API — wrapper for review endpoints.
 */
public class ReviewAPI extends BaseAPI {

    public Response addReview(String token, Map<String, Object> payload) {
        log.info("POST {} | bookId: {}", Endpoints.REVIEWS, payload.get("bookId"));
        return given().spec(authSpec(token)).body(payload).post(Endpoints.REVIEWS);
    }

    public Response deleteReview(String token, Long reviewId) {
        log.info("DELETE {} | id: {}", Endpoints.REVIEWS_BY_ID, reviewId);
        return given().spec(authSpec(token)).pathParam("id", reviewId).delete(Endpoints.REVIEWS_BY_ID);
    }

    public Response getBookReviews(Long bookId) {
        return given().spec(publicSpec).pathParam("bookId", bookId).get(Endpoints.REVIEWS_BY_BOOK);
    }

    public Response getBookRating(Long bookId) {
        return given().spec(publicSpec).pathParam("bookId", bookId).get(Endpoints.REVIEWS_RATING);
    }

    public Response addReviewWithoutToken(Map<String, Object> payload) {
        return given().spec(publicSpec).body(payload).post(Endpoints.REVIEWS);
    }
}
