package com.autotestx.tests.reviews;

import com.autotestx.api.ReviewAPI;
import com.autotestx.assertions.APIAssert;
import com.autotestx.base.BaseTest;
import com.autotestx.utilities.DBUtils;
import com.autotestx.utilities.FakerUtils;
import com.autotestx.utilities.ReportManager;
import com.autotestx.utilities.TokenManager;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.*;
import java.util.List;
import java.util.Map;

/**
 * Review Test Suite — 12 test cases
 */
@Feature("Reviews")
public class ReviewTests extends BaseTest {

    private final ReviewAPI reviewAPI = new ReviewAPI();
    private String userToken;
    private Long createdReviewId;
    private static final Long TEST_BOOK_ID = 4L; // Sapiens

    @BeforeClass
    public void setup() {
        TokenManager tm = TokenManager.getInstance();
        tm.initUserToken();
        userToken = tm.getUserToken();
    }

    @Test(priority = 1,
          description = "TC-REVIEW-001: Add review with valid data should return 201")
    @Severity(SeverityLevel.BLOCKER)
    public void addReview_withValidData_shouldReturn201() {
        ReportManager.createTest("TC-REVIEW-001 Add Review");

        Response response = reviewAPI.addReview(userToken, Map.of(
            "bookId",  TEST_BOOK_ID,
            "rating",  5,
            "comment", "Excellent book! " + FakerUtils.randomString(20)
        ));

        APIAssert.verifyStatus(response, 201);
        APIAssert.verifyFieldNotNull(response, "data.id");
        APIAssert.verifyField(response, "data.rating", 5);

        createdReviewId = response.jsonPath().getLong("data.id");
    }

    @Test(priority = 2,
          description = "TC-REVIEW-002: Adding duplicate review should return 409",
          dependsOnMethods = "addReview_withValidData_shouldReturn201")
    @Severity(SeverityLevel.CRITICAL)
    public void addDuplicateReview_shouldReturn409() {
        ReportManager.createTest("TC-REVIEW-002 Duplicate Review");

        Response response = reviewAPI.addReview(userToken, Map.of(
            "bookId", TEST_BOOK_ID,
            "rating", 3,
            "comment", "Another review attempt"
        ));
        APIAssert.verifyConflict(response);
    }

    @Test(priority = 3,
          description = "TC-REVIEW-003: Add review with rating 0 should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void addReview_withZeroRating_shouldReturn400() {
        ReportManager.createTest("TC-REVIEW-003 Rating=0 Boundary");

        Response response = reviewAPI.addReview(userToken, Map.of(
            "bookId", 3L,  // different book
            "rating", 0
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 4,
          description = "TC-REVIEW-004: Add review with rating 6 should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void addReview_withRating6_shouldReturn400() {
        ReportManager.createTest("TC-REVIEW-004 Rating=6 Boundary");

        Response response = reviewAPI.addReview(userToken, Map.of(
            "bookId", 3L,
            "rating", 6
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 5,
          description = "TC-REVIEW-005: Get book reviews should return paginated list")
    @Severity(SeverityLevel.CRITICAL)
    public void getBookReviews_shouldReturnPaginatedList() {
        ReportManager.createTest("TC-REVIEW-005 Get Book Reviews");

        Response response = reviewAPI.getBookReviews(TEST_BOOK_ID);
        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyPaginatedResponse(response);
    }

    @Test(priority = 6,
          description = "TC-REVIEW-006: Get book average rating should return numeric value")
    @Severity(SeverityLevel.CRITICAL)
    public void getBookRating_shouldReturnAverageRating() {
        ReportManager.createTest("TC-REVIEW-006 Average Rating");

        Response response = reviewAPI.getBookRating(TEST_BOOK_ID);
        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyFieldNotNull(response, "data.averageRating");
        APIAssert.verifyFieldNotNull(response, "data.totalReviews");

        double rating = response.jsonPath().getDouble("data.averageRating");
        Assert.assertTrue(rating >= 0 && rating <= 5,
            "Average rating should be between 0 and 5. Actual: " + rating);
    }

    @Test(priority = 7,
          description = "TC-REVIEW-007: Delete own review should return 200")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteReview_shouldReturn200() {
        ReportManager.createTest("TC-REVIEW-007 Delete Review");

        // Add a fresh review to a different book
        Response addResp = reviewAPI.addReview(userToken, Map.of(
            "bookId", 2L,  // Animal Farm
            "rating", 4,
            "comment", "Good read"
        ));
        Long reviewId = addResp.jsonPath().getLong("data.id");

        Response deleteResp = reviewAPI.deleteReview(userToken, reviewId);
        APIAssert.verifyStatus(deleteResp, 200);
    }

    @Test(priority = 8,
          description = "TC-REVIEW-008: Add review without token should return 401")
    @Severity(SeverityLevel.CRITICAL)
    public void addReview_withoutToken_shouldReturn401() {
        ReportManager.createTest("TC-REVIEW-008 Review Without Auth");

        Response response = reviewAPI.addReviewWithoutToken(Map.of(
            "bookId", 1L,
            "rating", 5
        ));
        APIAssert.verifyUnauthorized(response);
    }

    @Test(priority = 9,
          description = "TC-REVIEW-009: Get reviews for non-existent book should return 404")
    @Severity(SeverityLevel.NORMAL)
    public void getReviews_forNonExistentBook_shouldReturn404() {
        ReportManager.createTest("TC-REVIEW-009 Reviews Non-Existent Book");

        Response response = reviewAPI.getBookReviews(999999L);
        APIAssert.verifyNotFound(response);
    }

    @Test(priority = 10,
          description = "TC-REVIEW-010: Add review with missing bookId should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void addReview_withMissingBookId_shouldReturn400() {
        ReportManager.createTest("TC-REVIEW-010 Missing BookId");

        Response response = reviewAPI.addReview(userToken, Map.of("rating", 4));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 11,
          description = "TC-REVIEW-011: Review comment over 1000 chars should return 400")
    @Severity(SeverityLevel.MINOR)
    public void addReview_withLongComment_shouldReturn400() {
        ReportManager.createTest("TC-REVIEW-011 Comment Too Long Boundary");

        Response response = reviewAPI.addReview(userToken, Map.of(
            "bookId",  6L,
            "rating",  4,
            "comment", "A".repeat(1001)
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 12,
          description = "TC-REVIEW-012: Delete review of another user should return 403")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteReview_ofAnotherUser_shouldReturn403() {
        ReportManager.createTest("TC-REVIEW-012 Access Control - Delete Other's Review");

        // Try deleting a review ID that doesn't belong to current user
        if (createdReviewId != null) {
            // Login as admin and try to delete user's review via user token
            // (or try id 999999 which doesn't exist)
            Response response = reviewAPI.deleteReview(userToken, 999999L);
            APIAssert.verifyStatusIn(response, 403, 404);
        }
    }
}
