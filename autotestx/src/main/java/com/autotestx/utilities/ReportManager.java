package com.autotestx.utilities;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Extent Reports manager.
 * Creates HTML report with system info, environment, and styling.
 *
 * Usage:
 *   ExtentTest test = ReportManager.createTest("TC001 - Login with valid credentials");
 *   test.pass("Login successful");
 *   test.fail("Expected 200, got 401");
 *   ReportManager.flush();
 */
public final class ReportManager {

    private static final Logger log = LogManager.getLogger(ReportManager.class);
    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();

    private static final String REPORT_DIR = System.getProperty("report.dir", "reports");
    private static final String TIMESTAMP  = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

    private ReportManager() {}

    public static ExtentReports getInstance() {
        if (extentReports == null) {
            synchronized (ReportManager.class) {
                if (extentReports == null) {
                    initReports();
                }
            }
        }
        return extentReports;
    }

    private static void initReports() {
        String reportPath = REPORT_DIR + "/extent-report-" + TIMESTAMP + ".html";
        log.info("Initialising Extent Reports at: {}", reportPath);

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setTheme(Theme.DARK);
        sparkReporter.config().setDocumentTitle("AutoTestX — BookVerse API Test Report");
        sparkReporter.config().setReportName("API Automation Execution Report");
        sparkReporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);

        // System / environment info
        extentReports.setSystemInfo("Environment",  ConfigReader.getEnv().toUpperCase());
        extentReports.setSystemInfo("Base URL",     ConfigReader.get("base.url"));
        extentReports.setSystemInfo("Framework",    "AutoTestX v1.0");
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
        extentReports.setSystemInfo("OS",           System.getProperty("os.name"));
        extentReports.setSystemInfo("User",         System.getProperty("user.name"));
    }

    public static ExtentTest createTest(String testName) {
        ExtentTest test = getInstance().createTest(testName);
        testThread.set(test);
        return test;
    }

    public static ExtentTest createTest(String testName, String description) {
        ExtentTest test = getInstance().createTest(testName, description);
        testThread.set(test);
        return test;
    }

    public static ExtentTest getTest() {
        return testThread.get();
    }

    public static void flush() {
        if (extentReports != null) {
            extentReports.flush();
            log.info("Extent Report flushed to: {}/extent-report-{}.html", REPORT_DIR, TIMESTAMP);
        }
    }

    public static void removeTest() {
        testThread.remove();
    }
}
