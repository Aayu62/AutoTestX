package com.autotestx.listeners;

import com.autotestx.utilities.ReportManager;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.*;

/**
 * TestNG ITestListener implementation.
 * Integrates test lifecycle events with Extent Reports and Log4j2.
 *
 * Registered in testng.xml:
 *   <listeners>
 *     <listener class-name="com.autotestx.listeners.TestListener"/>
 *   </listeners>
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        log.info("▶ Starting: {} | {}", testName, description);

        ExtentTest test = ReportManager.createTest(
            "[" + result.getTestClass().getName().replace("com.autotestx.tests.", "") + "] " + testName,
            description != null ? description : ""
        );
        test.assignCategory(result.getTestClass().getRealClass().getSimpleName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("✅ PASSED: {}", result.getMethod().getMethodName());
        ExtentTest test = ReportManager.getTest();
        if (test != null) test.pass("Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("❌ FAILED: {} | Reason: {}",
                result.getMethod().getMethodName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "Unknown");

        ExtentTest test = ReportManager.getTest();
        if (test != null) {
            test.fail(result.getThrowable());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("⚠️  SKIPPED: {}", result.getMethod().getMethodName());
        ExtentTest test = ReportManager.getTest();
        if (test != null) test.log(Status.SKIP, "Test skipped");
    }

    @Override
    public void onFinish(ITestContext context) {
        int passed  = context.getPassedTests().size();
        int failed  = context.getFailedTests().size();
        int skipped = context.getSkippedTests().size();
        log.info("═══════════════════════════════════════════════════");
        log.info("  Suite: {} | Passed: {} | Failed: {} | Skipped: {}",
                context.getName(), passed, failed, skipped);
        log.info("═══════════════════════════════════════════════════");
    }

    @Override
    public void onStart(ISuite suite) {
        log.info("🚀 Suite started: {}", suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("🏁 Suite finished: {}", suite.getName());
    }
}
