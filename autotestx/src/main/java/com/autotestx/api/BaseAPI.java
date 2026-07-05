package com.autotestx.api;

import com.autotestx.utilities.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Base API class providing reusable RequestSpecification builders.
 *
 * All API classes extend this to get pre-configured specs with:
 *   - Base URI from config
 *   - Content-Type: application/json
 *   - Request/Response logging to file
 *   - Optional Bearer token injection
 */
public abstract class BaseAPI {

    protected static final Logger log = LogManager.getLogger(BaseAPI.class);

    protected static RequestSpecification publicSpec;
    protected static RequestSpecification authenticatedSpec;

    static {
        try {
            String baseUrl   = ConfigReader.get("base.url");
            int    timeout   = ConfigReader.getInt("request.timeout.ms");

            // Log requests/responses to file
            PrintStream logStream = new PrintStream(
                    new FileOutputStream("logs/api-requests.log", true));

            publicSpec = new RequestSpecBuilder()
                    .setBaseUri(baseUrl)
                    .setContentType(ContentType.JSON)
                    .setRelaxedHTTPSValidation()
                    .addFilter(new RequestLoggingFilter(LogDetail.ALL, logStream))
                    .addFilter(new ResponseLoggingFilter(LogDetail.ALL, logStream))
                    .build();

            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

            log.info("BaseAPI initialized | Base URL: {} | Timeout: {}ms", baseUrl, timeout);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize BaseAPI", e);
        }
    }

    /**
     * Build authenticated spec with Bearer token.
     */
    protected static RequestSpecification authSpec(String token) {
        return RestAssured.given()
                .spec(publicSpec)
                .header("Authorization", "Bearer " + token);
    }

    /**
     * Build spec with custom headers.
     */
    protected static RequestSpecification specWithHeaders(java.util.Map<String, String> headers) {
        RequestSpecification spec = RestAssured.given().spec(publicSpec);
        headers.forEach(spec::header);
        return spec;
    }

    /**
     * Default response spec (JSON content type).
     */
    protected static ResponseSpecification defaultResponseSpec() {
        return new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .build();
    }
}
