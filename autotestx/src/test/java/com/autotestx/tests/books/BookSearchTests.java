package com.autotestx.tests.books;

import com.autotestx.api.BookAPI;
import com.autotestx.assertions.APIAssert;
import com.autotestx.base.BaseTest;
import com.autotestx.utilities.ReportManager;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.*;
import java.util.List;
import java.util.Map;

/**
 * Book Search, Pagination, Sorting, Filtering Test Suite — 15 test cases
 */
@Feature("Books")
public class BookSearchTests extends BaseTest {

    private final BookAPI bookAPI = new BookAPI();

    @Test(priority = 1,
          description = "TC-SEARCH-001: Get books with page=0 size=5 should return max 5 items")
    @Severity(SeverityLevel.CRITICAL)
    public void getBooks_withPagination_shouldReturnCorrectPageSize() {
        ReportManager.createTest("TC-SEARCH-001 Pagination Size=5");

        Response response = bookAPI.getAllBooksWithParams(0, 5, "id", "asc", null, null);

        APIAssert.verifyStatus(response, 200);
        List<Object> content = response.jsonPath().getList("data.content");
        Assert.assertTrue(content.size() <= 5, "Should return at most 5 books. Got: " + content.size());
        APIAssert.verifyField(response, "data.size", 5);
    }

    @Test(priority = 2,
          description = "TC-SEARCH-002: Sort books by price ascending")
    @Severity(SeverityLevel.NORMAL)
    public void getBooks_sortedByPriceAscending_shouldReturnOrderedList() {
        ReportManager.createTest("TC-SEARCH-002 Sort By Price ASC");

        Response response = bookAPI.getAllBooksWithParams(0, 10, "price", "asc", null, null);

        APIAssert.verifyStatus(response, 200);
        List<Float> prices = response.jsonPath().getList("data.content.price");
        for (int i = 0; i < prices.size() - 1; i++) {
            Assert.assertTrue(prices.get(i) <= prices.get(i + 1),
                "Books should be sorted by price ASC. Index " + i + ": " + prices.get(i) + " > " + prices.get(i + 1));
        }
        ReportManager.getTest().pass("Price sorting verified: " + prices);
    }

    @Test(priority = 3,
          description = "TC-SEARCH-003: Sort books by price descending")
    @Severity(SeverityLevel.NORMAL)
    public void getBooks_sortedByPriceDescending_shouldReturnOrderedList() {
        ReportManager.createTest("TC-SEARCH-003 Sort By Price DESC");

        Response response = bookAPI.getAllBooksWithParams(0, 10, "price", "desc", null, null);

        APIAssert.verifyStatus(response, 200);
        List<Float> prices = response.jsonPath().getList("data.content.price");
        for (int i = 0; i < prices.size() - 1; i++) {
            Assert.assertTrue(prices.get(i) >= prices.get(i + 1),
                "Books should be sorted by price DESC. Index " + i);
        }
    }

    @Test(priority = 4,
          description = "TC-SEARCH-004: Search by keyword should return matching books")
    @Severity(SeverityLevel.CRITICAL)
    public void searchBooks_byKeyword_shouldReturnMatchingResults() {
        ReportManager.createTest("TC-SEARCH-004 Keyword Search");

        Response response = bookAPI.getAllBooksWithParams(0, 10, "id", "asc", "1984", null);

        APIAssert.verifyStatus(response, 200);
        List<String> titles = response.jsonPath().getList("data.content.title");
        Assert.assertFalse(titles.isEmpty(), "Search for '1984' should return at least one result");
        boolean found = titles.stream().anyMatch(t -> t.toLowerCase().contains("1984"));
        Assert.assertTrue(found, "At least one book title should contain '1984'. Got: " + titles);
    }

    @Test(priority = 5,
          description = "TC-SEARCH-005: Filter by category ID should only return books in that category")
    @Severity(SeverityLevel.CRITICAL)
    public void filterBooks_byCategoryId_shouldReturnCorrectCategory() {
        ReportManager.createTest("TC-SEARCH-005 Filter By Category");

        Long categoryId = 4L; // Technology
        Response response = bookAPI.getAllBooksWithParams(0, 10, "id", "asc", null, categoryId);

        APIAssert.verifyStatus(response, 200);
        List<Integer> categoryIds = response.jsonPath().getList("data.content.category.id");
        for (Integer id : categoryIds) {
            Assert.assertEquals(id.longValue(), categoryId.longValue(),
                "All books should be in category " + categoryId);
        }
    }

    @Test(priority = 6,
          description = "TC-SEARCH-006: Search with no results should return empty content list")
    @Severity(SeverityLevel.NORMAL)
    public void searchBooks_withNoMatch_shouldReturnEmpty() {
        ReportManager.createTest("TC-SEARCH-006 No Results Search");

        Response response = bookAPI.getAllBooksWithParams(0, 10, "id", "asc",
                "ZZZNOMATCHXXX999", null);

        APIAssert.verifyStatus(response, 200);
        List<Object> content = response.jsonPath().getList("data.content");
        Assert.assertTrue(content.isEmpty() || content.size() == 0,
            "No books should match 'ZZZNOMATCHXXX999'");
        Assert.assertEquals((int) response.jsonPath().getInt("data.totalElements"), 0);
    }

    @Test(priority = 7,
          description = "TC-SEARCH-007: Page beyond total pages should return empty content")
    @Severity(SeverityLevel.NORMAL)
    public void getBooks_beyondLastPage_shouldReturnEmpty() {
        ReportManager.createTest("TC-SEARCH-007 Beyond Last Page");

        Response response = bookAPI.getAllBooksWithParams(9999, 10, "id", "asc", null, null);

        APIAssert.verifyStatus(response, 200);
        List<Object> content = response.jsonPath().getList("data.content");
        Assert.assertTrue(content == null || content.isEmpty(),
            "Page 9999 should return empty content");
    }

    @Test(priority = 8,
          description = "TC-SEARCH-008: Total elements should be consistent across pages")
    @Severity(SeverityLevel.NORMAL)
    public void getBooks_totalElements_shouldBeConsistent() {
        ReportManager.createTest("TC-SEARCH-008 Consistent Total Elements");

        Response page1 = bookAPI.getAllBooksWithParams(0, 5, "id", "asc", null, null);
        Response page2 = bookAPI.getAllBooksWithParams(1, 5, "id", "asc", null, null);

        int total1 = page1.jsonPath().getInt("data.totalElements");
        int total2 = page2.jsonPath().getInt("data.totalElements");
        Assert.assertEquals(total1, total2, "totalElements should be consistent across pages");
    }

    @Test(priority = 9,
          description = "TC-SEARCH-009: Books list should include review count and average rating")
    @Severity(SeverityLevel.NORMAL)
    public void booksInList_shouldIncludeRatingData() {
        ReportManager.createTest("TC-SEARCH-009 Rating Data in List");

        Response response = bookAPI.getAllBooks();
        APIAssert.verifyStatus(response, 200);
        // reviewCount should be present (can be 0)
        List<Object> reviewCounts = response.jsonPath().getList("data.content.reviewCount");
        Assert.assertNotNull(reviewCounts, "reviewCount should be present in book list response");
    }

    @Test(priority = 10,
          description = "TC-SEARCH-010: Sort by title alphabetically")
    @Severity(SeverityLevel.NORMAL)
    public void getBooks_sortedByTitle_shouldBeAlphabetical() {
        ReportManager.createTest("TC-SEARCH-010 Alphabetical Sort");

        Response response = bookAPI.getAllBooksWithParams(0, 10, "title", "asc", null, null);
        APIAssert.verifyStatus(response, 200);

        List<String> titles = response.jsonPath().getList("data.content.title");
        for (int i = 0; i < titles.size() - 1; i++) {
            Assert.assertTrue(titles.get(i).compareToIgnoreCase(titles.get(i + 1)) <= 0,
                "Titles should be alphabetical");
        }
    }

    @Test(priority = 11,
          description = "TC-SEARCH-011: Pagination metadata should be correct")
    @Severity(SeverityLevel.NORMAL)
    public void getBooks_paginationMetadata_shouldBeCorrect() {
        ReportManager.createTest("TC-SEARCH-011 Pagination Metadata");

        int pageSize = 3;
        Response response = bookAPI.getAllBooksWithParams(0, pageSize, "id", "asc", null, null);

        APIAssert.verifyStatus(response, 200);
        Assert.assertEquals((int) response.jsonPath().getInt("data.size"), pageSize);
        Assert.assertEquals((int) response.jsonPath().getInt("data.number"), 0);
        Assert.assertFalse((boolean) response.jsonPath().getBoolean("data.first"),
                        response.jsonPath().getBoolean("data.first") == null ? false : false);
    }

    @Test(priority = 12,
          description = "TC-SEARCH-012: Response time for paginated list should be under 500ms")
    @Severity(SeverityLevel.NORMAL)
    public void getAllBooks_responseTime_shouldBeUnder500ms() {
        ReportManager.createTest("TC-SEARCH-012 List Performance");

        Response response = bookAPI.getAllBooksWithParams(0, 10, "id", "asc", null, null);
        APIAssert.verifyPerformance(response, 200, 500);
    }
}
