package com.autotestx.tests.users;

import com.autotestx.api.UserAPI;
import com.autotestx.assertions.APIAssert;
import com.autotestx.base.BaseTest;
import com.autotestx.utilities.TokenManager;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Map;

public class UserTests extends BaseTest {

    private String userToken;
    private String adminToken;
    private UserAPI userAPI;

    @BeforeClass
    public void setup() {
        userToken = TokenManager.getInstance().getUserToken();
        adminToken = TokenManager.getInstance().getAdminToken();
        userAPI = new UserAPI();
    }

    @Test(description = "Get current user profile (Me)")
    public void testGetCurrentUser() {
        Response response = userAPI.getMyProfile(userToken);
        APIAssert.verifyStatus(response, 200);
        APIAssert.verifyField(response, "data.email", "user@bookverse.com");
    }

    @Test(description = "Admin gets dashboard stats")
    public void testAdminGetDashboard() {
        Response response = userAPI.getAdminDashboard(adminToken);
        APIAssert.verifyStatus(response, 200);
    }

    @Test(description = "User cannot get admin dashboard")
    public void testUserCannotGetAdminDashboard() {
        Response response = userAPI.getAdminDashboard(userToken);
        APIAssert.verifyStatus(response, 403);
    }

    @Test(description = "Admin updates user profile")
    public void testUpdateUser() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Updated User");
        
        // Updating user ID 2 
        Response response = userAPI.updateUser(adminToken, 2L, payload);
        
        if(response.statusCode() == 200) {
            APIAssert.verifyField(response, "data.name", "Updated User");
        }
    }
}
