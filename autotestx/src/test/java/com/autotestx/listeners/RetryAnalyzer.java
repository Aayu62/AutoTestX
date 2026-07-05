package com.autotestx.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retry analyzer for flaky API tests.
 * Automatically retries failed tests up to MAX_RETRY times.
 *
 * Usage on individual tests:
 *   @Test(retryAnalyzer = RetryAnalyzer.class)
 *
 * Or configure globally in testng.xml via listener.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);
    private static final int MAX_RETRY = 3;

    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY) {
            retryCount++;
            log.warn("🔄 Retrying test: {} | Attempt: {}/{}",
                    result.getMethod().getMethodName(), retryCount, MAX_RETRY);
            return true;
        }
        log.error("💥 Test exhausted retries: {}", result.getMethod().getMethodName());
        return false;
    }
}
