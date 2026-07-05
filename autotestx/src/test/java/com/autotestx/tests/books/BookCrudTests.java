package com.autotestx.tests.books;

import com.autotestx.api.BookAPI;
import com.autotestx.assertions.APIAssert;
import com.autotestx.base.BaseTest;
import com.autotestx.listeners.RetryAnalyzer;
import com.autotestx.utilities.DBUtils;
import com.autotestx.utilities.FakerUtils;
import com.autotestx.utilities.ReportManager;
import com.autotestx.utilities.TokenManager;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Book CRUD Test Suite — 20 test cases
 * Covers: create, read, update, delete + DB validation
 */
@Feature("Books")
public class BookCrudTests extends BaseTest {

    private final BookAPI bookAPI = new BookAPI();
    private String adminToken;
    private Long createdBookId;

    @BeforeClass
    public void setup() {
        TokenManager tm = TokenManager.getInstance();
        tm.initAdminToken();
        adminToken = tm.getAdminToken();
    }

    private Map<String, Object> validBookPayload() {
        return new HashMap<>(Map.of(
            "title",       FakerUtils.randomBookTitle(),
            "isbn",        FakerUtils.randomISBN(),
            "description", FakerUtils.randomDescription(),
            "price",       FakerUtils.randomPrice(),
            "stock",       FakerUtils.randomStock(),
            "authorId",    1L,
            "categoryId",  1L
        ));
    }

    @Test(priority = 1,
          description = "TC-BOOK-001: Create book with valid data should return 201",
          retryAnalyzer = RetryAnalyzer.class)
    @Severity(SeverityLevel.BLOCKER)
    public void createBook_withValidData_shouldReturn201() {
        ReportManager.createTest("TC-BOOK-001 Create Book Valid");

        Map<String, Object> payload = validBookPayload();
        Response response = bookAPI.createBook(adminToken, payload);

        APIAssert.verifyBookCreated(response);
        APIAssert.verifyField(response, "data.title", payload.get("title"));
        APIAssert.verifyField(response, "data.isbn", payload.get("isbn"));
        APIAssert.verifyResponseTime(response, 2000);

        createdBookId = response.jsonPath().getLong("data.id");

        // Database validation — this is what separates junior from senior testers
        Assert.assertTrue(DBUtils.getInstance().verifyBookExists(createdBookId),
            "Book should exist in PostgreSQL after creation. ID: " + createdBookId);

        Map<String, Object> dbBook = DBUtils.getInstance().getBookFromDB(createdBookId);
        Assert.assertEquals(dbBook.get("title"), payload.get("title"),
            "Title in DB should match API payload");

        ReportManager.getTest().pass("✅ DB Validation passed: book exists in PostgreSQL");
    }

    @Test(priority = 2,
          description = "TC-BOOK-002: Get all books should return 200 and paginated list",
          dependsOnMethods = "createBook_withValidData_shouldReturn201")
    @Severity(SeverityLevel.BLOCKER)
    public void getAllBooks_shouldReturn200WithPagination() {
        ReportManager.createTest("TC-BOOK-002 Get All Books");

        Response response = bookAPI.getAllBooks();

        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyPaginatedResponse(response);
        APIAssert.verifyResponseTime(response, 500);
    }

    @Test(priority = 3,
          description = "TC-BOOK-003: Get book by valid ID should return 200",
          dependsOnMethods = "createBook_withValidData_shouldReturn201")
    @Severity(SeverityLevel.BLOCKER)
    public void getBookById_withValidId_shouldReturn200() {
        ReportManager.createTest("TC-BOOK-003 Get Book By ID");

        Response response = bookAPI.getBookById(createdBookId);

        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyField(response, "data.id", createdBookId.intValue());
        APIAssert.verifyFieldNotNull(response, "data.title");
        APIAssert.verifyFieldNotNull(response, "data.author");
        APIAssert.verifyFieldNotNull(response, "data.category");
    }

    @Test(priority = 4,
          description = "TC-BOOK-004: Get book by ID should include author and category details")
    @Severity(SeverityLevel.NORMAL)
    public void getBookById_shouldIncludeAuthorAndCategory() {
        ReportManager.createTest("TC-BOOK-004 Book Nested Objects");

        Response response = bookAPI.getBookById(1L);
        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyFieldNotNull(response, "data.author.id");
        APIAssert.verifyFieldNotNull(response, "data.author.name");
        APIAssert.verifyFieldNotNull(response, "data.category.id");
        APIAssert.verifyFieldNotNull(response, "data.category.name");
    }

    @Test(priority = 5,
          description = "TC-BOOK-005: Book response should match JSON schema",
          dependsOnMethods = "createBook_withValidData_shouldReturn201")
    @Severity(SeverityLevel.NORMAL)
    public void getBookById_shouldMatchSchema() {
        ReportManager.createTest("TC-BOOK-005 Book Schema Validation");

        Response response = bookAPI.getBookById(createdBookId);
        APIAssert.verifyStatus(response, 200);
        APIAssert.verifySchema(response, "book-schema.json");
    }

    @Test(priority = 6,
          description = "TC-BOOK-006: Update book should return 200 with updated data",
          dependsOnMethods = "createBook_withValidData_shouldReturn201")
    @Severity(SeverityLevel.CRITICAL)
    public void updateBook_withValidData_shouldReturn200() {
        ReportManager.createTest("TC-BOOK-006 Update Book");

        String updatedTitle = "Updated: " + FakerUtils.randomBookTitle();
        Map<String, Object> updatePayload = validBookPayload();
        updatePayload.put("title", updatedTitle);

        Response response = bookAPI.updateBook(adminToken, createdBookId, updatePayload);

        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyField(response, "data.title", updatedTitle);

        // Verify update in DB
        Map<String, Object> dbBook = DBUtils.getInstance().getBookFromDB(createdBookId);
        Assert.assertEquals(dbBook.get("title"), updatedTitle, "Updated title should persist in DB");
    }

    @Test(priority = 7,
          description = "TC-BOOK-007: Update stock should reflect in inventory",
          dependsOnMethods = "createBook_withValidData_shouldReturn201")
    @Severity(SeverityLevel.NORMAL)
    public void updateBookStock_shouldReflectInDB() {
        ReportManager.createTest("TC-BOOK-007 Stock Update DB Validation");

        int originalStock = DBUtils.getInstance().getBookStock(createdBookId);
        Map<String, Object> payload = validBookPayload();
        payload.put("stock", 999);

        bookAPI.updateBook(adminToken, createdBookId, payload);

        int newStock = DBUtils.getInstance().getBookStock(createdBookId);
        Assert.assertEquals(newStock, 999, "Stock should be updated to 999 in DB");
    }

    @Test(priority = 20,
          description = "TC-BOOK-020: Delete book should return 200",
          dependsOnMethods = "createBook_withValidData_shouldReturn201")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteBook_shouldReturn200() {
        ReportManager.createTest("TC-BOOK-020 Delete Book");

        // Create a fresh book to delete
        Response createResp = bookAPI.createBook(adminToken, validBookPayload());
        Long bookToDelete = createResp.jsonPath().getLong("data.id");

        Response deleteResp = bookAPI.deleteBook(adminToken, bookToDelete);
        APIAssert.verifyStatus(deleteResp, 200);

        // Verify deleted from DB
        Assert.assertFalse(DBUtils.getInstance().verifyBookExists(bookToDelete),
            "Book should NOT exist in DB after deletion");
        ReportManager.getTest().pass("✅ DB Validation: book removed from PostgreSQL");
    }

    @Test(priority = 8,
          description = "TC-BOOK-008: Create book without token should return 403")
    @Severity(SeverityLevel.CRITICAL)
    public void createBook_withoutToken_shouldReturn403() {
        ReportManager.createTest("TC-BOOK-008 Create Without Auth");

        Response response = bookAPI.createBookWithoutToken(validBookPayload());
        APIAssert.verifyStatusIn(response, 401, 403);
    }

    @Test(priority = 9,
          description = "TC-BOOK-009: Create book with user token (not admin) should return 403")
    @Severity(SeverityLevel.CRITICAL)
    public void createBook_withUserToken_shouldReturn403() {
        ReportManager.createTest("TC-BOOK-009 Create With User Token (Non-Admin)");

        TokenManager.getInstance().initUserToken();
        String userToken = TokenManager.getInstance().getUserToken();

        Response response = bookAPI.createBook(userToken, validBookPayload());
        APIAssert.verifyForbidden(response);
    }

    @Test(priority = 10,
          description = "TC-BOOK-010: Create book with duplicate ISBN should return 409")
    @Severity(SeverityLevel.CRITICAL)
    public void createBook_withDuplicateISBN_shouldReturn409() {
        ReportManager.createTest("TC-BOOK-010 Duplicate ISBN");

        Map<String, Object> payload = validBookPayload();
        bookAPI.createBook(adminToken, payload); // first creation

        Response response = bookAPI.createBook(adminToken, payload); // same ISBN
        APIAssert.verifyConflict(response);
    }

    @Test(priority = 11,
          description = "TC-BOOK-011: Create book with negative price should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void createBook_withNegativePrice_shouldReturn400() {
        ReportManager.createTest("TC-BOOK-011 Negative Price Validation");

        Map<String, Object> payload = validBookPayload();
        payload.put("price", -9.99);

        Response response = bookAPI.createBook(adminToken, payload);
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 12,
          description = "TC-BOOK-012: Create book with zero price should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void createBook_withZeroPrice_shouldReturn400() {
        ReportManager.createTest("TC-BOOK-012 Zero Price Boundary");

        Map<String, Object> payload = validBookPayload();
        payload.put("price", 0.00);

        Response response = bookAPI.createBook(adminToken, payload);
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 13,
          description = "TC-BOOK-013: Create book with missing title should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void createBook_withMissingTitle_shouldReturn400() {
        ReportManager.createTest("TC-BOOK-013 Missing Title");

        Map<String, Object> payload = validBookPayload();
        payload.remove("title");

        Response response = bookAPI.createBook(adminToken, payload);
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 14,
          description = "TC-BOOK-014: Create book with invalid author ID should return 404")
    @Severity(SeverityLevel.NORMAL)
    public void createBook_withInvalidAuthorId_shouldReturn404() {
        ReportManager.createTest("TC-BOOK-014 Invalid Author ID");

        Map<String, Object> payload = validBookPayload();
        payload.put("authorId", 99999L);

        Response response = bookAPI.createBook(adminToken, payload);
        APIAssert.verifyNotFound(response);
    }

    @Test(priority = 15,
          description = "TC-BOOK-015: Get book by non-existent ID should return 404")
    @Severity(SeverityLevel.NORMAL)
    public void getBookById_withNonExistentId_shouldReturn404() {
        ReportManager.createTest("TC-BOOK-015 Book Not Found");

        Response response = bookAPI.getBookById(999999L);
        APIAssert.verifyNotFound(response);
    }

    @Test(priority = 16,
          description = "TC-BOOK-016: Price should be non-negative in response")
    @Severity(SeverityLevel.NORMAL)
    public void bookPrice_shouldBePositive() {
        ReportManager.createTest("TC-BOOK-016 Price Positive");

        Response response = bookAPI.getBookById(1L);
        float price = response.jsonPath().getFloat("data.price");
        Assert.assertTrue(price > 0, "Book price should be positive. Actual: " + price);
    }

    @Test(priority = 17,
          description = "TC-BOOK-017: Stock should be non-negative in response")
    @Severity(SeverityLevel.NORMAL)
    public void bookStock_shouldBeNonNegative() {
        ReportManager.createTest("TC-BOOK-017 Stock Non-Negative");

        Response response = bookAPI.getBookById(1L);
        int stock = response.jsonPath().getInt("data.stock");
        Assert.assertTrue(stock >= 0, "Book stock should be non-negative. Actual: " + stock);
    }

    @Test(priority = 18,
          description = "TC-BOOK-018: Book with negative stock should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void createBook_withNegativeStock_shouldReturn400() {
        ReportManager.createTest("TC-BOOK-018 Negative Stock Boundary");

        Map<String, Object> payload = validBookPayload();
        payload.put("stock", -1);

        Response response = bookAPI.createBook(adminToken, payload);
        APIAssert.verifyBadRequest(response);
    }
}
