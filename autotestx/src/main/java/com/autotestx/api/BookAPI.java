package com.autotestx.api;

import com.autotestx.constants.Endpoints;
import io.restassured.response.Response;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Book API — wrapper for all book endpoints.
 *
 * GET    /api/books                    (public)
 * GET    /api/books/{id}              (public)
 * POST   /api/books                   (admin)
 * PUT    /api/books/{id}              (admin)
 * DELETE /api/books/{id}              (admin)
 */
public class BookAPI extends BaseAPI {

    public Response getAllBooks() {
        log.info("GET {}", Endpoints.BOOKS);
        return given().spec(publicSpec).get(Endpoints.BOOKS);
    }

    public Response getAllBooks(Map<String, Object> queryParams) {
        log.info("GET {} | params: {}", Endpoints.BOOKS, queryParams);
        return given().spec(publicSpec).queryParams(queryParams).get(Endpoints.BOOKS);
    }

    public Response getAllBooksWithParams(int page, int size, String sortBy, String sortDir,
                                          String search, Long categoryId) {
        var req = given().spec(publicSpec)
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sortBy", sortBy)
                .queryParam("sortDir", sortDir);
        if (search    != null) req.queryParam("search", search);
        if (categoryId != null) req.queryParam("categoryId", categoryId);
        return req.get(Endpoints.BOOKS);
    }

    public Response getBookById(Long id) {
        log.info("GET {} | id: {}", Endpoints.BOOKS_BY_ID, id);
        return given().spec(publicSpec).pathParam("id", id).get(Endpoints.BOOKS_BY_ID);
    }

    public Response createBook(String token, Map<String, Object> payload) {
        log.info("POST {} | title: {}", Endpoints.BOOKS, payload.get("title"));
        return given().spec(authSpec(token)).body(payload).post(Endpoints.BOOKS);
    }

    public Response updateBook(String token, Long id, Map<String, Object> payload) {
        log.info("PUT {} | id: {}", Endpoints.BOOKS_BY_ID, id);
        return given().spec(authSpec(token)).pathParam("id", id).body(payload).put(Endpoints.BOOKS_BY_ID);
    }

    public Response deleteBook(String token, Long id) {
        log.info("DELETE {} | id: {}", Endpoints.BOOKS_BY_ID, id);
        return given().spec(authSpec(token)).pathParam("id", id).delete(Endpoints.BOOKS_BY_ID);
    }

    // Negative test helpers
    public Response createBookWithoutToken(Map<String, Object> payload) {
        return given().spec(publicSpec).body(payload).post(Endpoints.BOOKS);
    }

    public Response getBookById(String invalidId) {
        return given().spec(publicSpec).pathParam("id", invalidId).get(Endpoints.BOOKS_BY_ID);
    }
}
