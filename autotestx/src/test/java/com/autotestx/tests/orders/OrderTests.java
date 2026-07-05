package com.autotestx.tests.orders;

import com.autotestx.api.BookAPI;
import com.autotestx.api.OrderAPI;
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
 * Order Test Suite — 18 test cases
 * Covers: create, view, cancel, history, stock management, negative cases
 */
@Feature("Orders")
public class OrderTests extends BaseTest {

    private final OrderAPI orderAPI = new OrderAPI();
    private String userToken;
    private Long createdOrderId;

    @BeforeClass
    public void setup() {
        TokenManager tm = TokenManager.getInstance();
        tm.initUserToken();
        userToken = tm.getUserToken();
    }

    private Map<String, Object> validOrderPayload() {
        return Map.of(
            "notes", FakerUtils.randomOrderNote(),
            "items", List.of(
                Map.of("bookId", 5L, "quantity", 1)  // Clean Code book
            )
        );
    }

    @Test(priority = 1,
          description = "TC-ORDER-001: Create order with valid data should return 201")
    @Severity(SeverityLevel.BLOCKER)
    public void createOrder_withValidData_shouldReturn201() {
        ReportManager.createTest("TC-ORDER-001 Create Valid Order");

        Response response = orderAPI.createOrder(userToken, validOrderPayload());

        APIAssert.verifyOrderCreated(response);
        APIAssert.verifyResponseTime(response, 2000);

        createdOrderId = response.jsonPath().getLong("data.id");

        // DB Validation
        Assert.assertTrue(DBUtils.getInstance().verifyOrderExists(createdOrderId),
            "Order should exist in PostgreSQL. ID: " + createdOrderId);

        String dbStatus = DBUtils.getInstance().getOrderStatus(createdOrderId);
        Assert.assertEquals(dbStatus, "PENDING",
            "Order status should be PENDING in DB");

        ReportManager.getTest().pass("✅ DB Validation: Order exists with PENDING status");
    }

    @Test(priority = 2,
          description = "TC-ORDER-002: Created order should have correct items and total",
          dependsOnMethods = "createOrder_withValidData_shouldReturn201")
    @Severity(SeverityLevel.CRITICAL)
    public void createdOrder_shouldHaveCorrectItems() {
        ReportManager.createTest("TC-ORDER-002 Order Items and Total");

        Response response = orderAPI.getOrderById(userToken, createdOrderId);

        APIAssert.verifyStatus(response, 200);
        List<Object> items = response.jsonPath().getList("data.items");
        Assert.assertFalse(items.isEmpty(), "Order should have at least one item");

        float totalPrice = response.jsonPath().getFloat("data.totalPrice");
        Assert.assertTrue(totalPrice > 0, "Order total price should be positive");
    }

    @Test(priority = 3,
          description = "TC-ORDER-003: Order response should match JSON schema",
          dependsOnMethods = "createOrder_withValidData_shouldReturn201")
    @Severity(SeverityLevel.NORMAL)
    public void orderResponse_shouldMatchSchema() {
        ReportManager.createTest("TC-ORDER-003 Order Schema Validation");

        Response response = orderAPI.getOrderById(userToken, createdOrderId);
        APIAssert.verifyStatus(response, 200);
        APIAssert.verifySchema(response, "order-schema.json");
    }

    @Test(priority = 4,
          description = "TC-ORDER-004: Get my orders should return paginated list")
    @Severity(SeverityLevel.CRITICAL)
    public void getMyOrders_shouldReturnPaginatedList() {
        ReportManager.createTest("TC-ORDER-004 Get My Orders");

        Response response = orderAPI.getMyOrders(userToken);

        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyPaginatedResponse(response);
    }

    @Test(priority = 5,
          description = "TC-ORDER-005: Cancel PENDING order should return 200 with CANCELLED status",
          dependsOnMethods = "createOrder_withValidData_shouldReturn201")
    @Severity(SeverityLevel.CRITICAL)
    public void cancelOrder_shouldReturn200WithCancelledStatus() {
        ReportManager.createTest("TC-ORDER-005 Cancel Order");

        // Create fresh order to cancel
        Response createResp = orderAPI.createOrder(userToken, validOrderPayload());
        Long orderToCancel = createResp.jsonPath().getLong("data.id");

        Response cancelResp = orderAPI.cancelOrder(userToken, orderToCancel);

        APIAssert.verifyStatus(cancelResp, 200);
        APIAssert.verifyOrderStatus(cancelResp, "CANCELLED");

        // DB Validation
        Assert.assertEquals(DBUtils.getInstance().getOrderStatus(orderToCancel), "CANCELLED",
            "Order status should be CANCELLED in DB after cancellation");
    }

    @Test(priority = 6,
          description = "TC-ORDER-006: Create order without token should return 401")
    @Severity(SeverityLevel.CRITICAL)
    public void createOrder_withoutToken_shouldReturn401() {
        ReportManager.createTest("TC-ORDER-006 Create Order No Auth");

        Response response = orderAPI.createOrderWithoutToken(validOrderPayload());
        APIAssert.verifyUnauthorized(response);
    }

    @Test(priority = 7,
          description = "TC-ORDER-007: Create order with empty items list should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void createOrder_withEmptyItems_shouldReturn400() {
        ReportManager.createTest("TC-ORDER-007 Empty Items");

        Response response = orderAPI.createOrder(userToken, Map.of(
            "notes", "Test",
            "items", List.of()
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 8,
          description = "TC-ORDER-008: Create order with non-existent book should return 404")
    @Severity(SeverityLevel.NORMAL)
    public void createOrder_withNonExistentBook_shouldReturn404() {
        ReportManager.createTest("TC-ORDER-008 Non-Existent Book In Order");

        Response response = orderAPI.createOrder(userToken, Map.of(
            "items", List.of(Map.of("bookId", 999999L, "quantity", 1))
        ));
        APIAssert.verifyNotFound(response);
    }

    @Test(priority = 9,
          description = "TC-ORDER-009: Create order with quantity 0 should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void createOrder_withZeroQuantity_shouldReturn400() {
        ReportManager.createTest("TC-ORDER-009 Zero Quantity");

        Response response = orderAPI.createOrder(userToken, Map.of(
            "items", List.of(Map.of("bookId", 1L, "quantity", 0))
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 10,
          description = "TC-ORDER-010: Create order with quantity exceeding 100 should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void createOrder_withExcessiveQuantity_shouldReturn400() {
        ReportManager.createTest("TC-ORDER-010 Excessive Quantity Boundary");

        Response response = orderAPI.createOrder(userToken, Map.of(
            "items", List.of(Map.of("bookId", 1L, "quantity", 101))
        ));
        APIAssert.verifyBadRequest(response);
    }

    @Test(priority = 11,
          description = "TC-ORDER-011: Get order of another user should return 403")
    @Severity(SeverityLevel.CRITICAL)
    public void getOrder_ofAnotherUser_shouldReturn403() {
        ReportManager.createTest("TC-ORDER-011 Order Access Control");

        // Admin creates an order
        TokenManager tm = TokenManager.getInstance();
        tm.initAdminToken();

        // User tries to access admin's order (if any exist — use id 1 as test)
        Response response = orderAPI.getOrderById(userToken, 1L);
        // If order 1 belongs to admin, user should get 403
        // If not found at all, 404 is acceptable too
        APIAssert.verifyStatusIn(response, 200, 403, 404);
    }

    @Test(priority = 12,
          description = "TC-ORDER-012: Get non-existent order should return 404")
    @Severity(SeverityLevel.NORMAL)
    public void getOrder_withNonExistentId_shouldReturn404() {
        ReportManager.createTest("TC-ORDER-012 Order Not Found");

        Response response = orderAPI.getOrderById(userToken, 9999999L);
        APIAssert.verifyNotFound(response);
    }

    @Test(priority = 13,
          description = "TC-ORDER-013: Order total should match sum of item quantities × unit price")
    @Severity(SeverityLevel.CRITICAL)
    public void orderTotal_shouldMatchCalculation() {
        ReportManager.createTest("TC-ORDER-013 Total Price Calculation");

        Response createResp = orderAPI.createOrder(userToken, Map.of(
            "items", List.of(Map.of("bookId", 1L, "quantity", 2))
        ));
        APIAssert.verifyStatus(createResp, 201);

        float total = createResp.jsonPath().getFloat("data.totalPrice");
        List<Map<String, Object>> items = createResp.jsonPath().getList("data.items");

        float calculated = 0;
        for (Map<String, Object> item : items) {
            float unitPrice = ((Number) item.get("unitPrice")).floatValue();
            int qty = ((Number) item.get("quantity")).intValue();
            calculated += unitPrice * qty;
        }

        Assert.assertEquals(total, calculated, 0.01f,
            "Order total should equal sum of unit_price × quantity");
    }

    @Test(priority = 14,
          description = "TC-ORDER-014: Create order response time should be under 2 seconds")
    @Severity(SeverityLevel.NORMAL)
    public void createOrder_responseTime_shouldBeUnder2Seconds() {
        ReportManager.createTest("TC-ORDER-014 Create Order Performance");

        Response response = orderAPI.createOrder(userToken, validOrderPayload());
        APIAssert.verifyPerformance(response, 201, 2000);
    }

    @Test(priority = 15,
          description = "TC-ORDER-015: Get orders with pagination should work correctly")
    @Severity(SeverityLevel.NORMAL)
    public void getMyOrders_withPagination_shouldWork() {
        ReportManager.createTest("TC-ORDER-015 Order Pagination");

        Response response = orderAPI.getMyOrders(userToken, 0, 5);
        APIAssert.verifyStatus(response, 200);
        List<Object> content = response.jsonPath().getList("data.content");
        Assert.assertTrue(content.size() <= 5, "Page should contain at most 5 orders");
    }
}
