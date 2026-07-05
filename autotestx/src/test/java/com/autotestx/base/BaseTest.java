package com.autotestx.base;

import com.autotestx.utilities.ConfigReader;
import com.autotestx.utilities.DBUtils;
import com.autotestx.utilities.ReportManager;
import com.autotestx.utilities.TokenManager;
import io.restassured.RestAssured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.io.File;

/**
 * Base test class for all AutoTestX test suites.
 *
 * Responsibilities:
 * - Configure RestAssured base URI and port
 * - Initialize user and admin JWT tokens (once per suite)
 * - Create required directories
 * - Initialize DB connection
 * - Flush Extent Reports after all tests
 */
public class BaseTest {

    protected static final Logger log = LogManager.getLogger(BaseTest.class);

    @BeforeSuite(alwaysRun = true)
    public void globalSetup() {
        log.info("========================================");
        log.info("  AutoTestX — Test Suite Starting");
        log.info("  Environment: {}", ConfigReader.getEnv().toUpperCase());
        log.info("  Base URL:    {}", ConfigReader.get("base.url"));
        log.info("========================================");

        // Create required directories
        createDirectory("reports");
        createDirectory("logs");

        // Configure RestAssured globally
        RestAssured.baseURI = ConfigReader.get("base.url");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Initialize tokens (login once for entire suite)
        log.info("Initializing user tokens...");
        try {
            TokenManager.getInstance().initUserToken();
            log.info("✅ User token initialized");
        } catch (Exception e) {
            log.warn("⚠️  User token initialization failed: {}", e.getMessage());
        }

        try {
            TokenManager.getInstance().initAdminToken();
            log.info("✅ Admin token initialized");
        } catch (Exception e) {
            log.warn("⚠️  Admin token initialization failed: {}", e.getMessage());
        }

        // Initialize report
        ReportManager.getInstance();

        log.info("Global setup complete. Tests starting...");
    }

    @AfterSuite(alwaysRun = true)
    public void globalTeardown() {
        log.info("All tests completed. Generating reports...");
        ReportManager.flush();
        DBUtils.getInstance().closeConnection();
        log.info("========================================");
        log.info("  AutoTestX — Test Suite Complete");
        log.info("  Reports: reports/");
        log.info("========================================");
    }

    private void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists() && dir.mkdirs()) {
            log.debug("Created directory: {}", path);
        }
    }
}
