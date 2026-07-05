package com.autotestx.utilities;

import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/**
 * JavaFaker wrapper for generating random, realistic test data.
 * Eliminates duplicate data conflicts in data-driven tests.
 *
 * Usage:
 *   FakerUtils.randomEmail()        → "john.smith.abc123@test.com"
 *   FakerUtils.randomBookTitle()    → "The Invisible Forest"
 *   FakerUtils.randomISBN()         → "978-0123456789"
 *   FakerUtils.randomPrice()        → 24.99
 */
public final class FakerUtils {

    private static final Faker FAKER = new Faker();
    private static final String TEST_EMAIL_DOMAIN = "@autotestx.test";

    private FakerUtils() {}

    // ── User Data ────────────────────────────────────────────────────────────────
    public static String randomEmail() {
        return (FAKER.name().firstName() + "." +
                FAKER.name().lastName() + "." +
                FAKER.number().digits(5))
                .toLowerCase()
                .replaceAll("[^a-z0-9.]", "") + TEST_EMAIL_DOMAIN;
    }

    public static String randomName() {
        return FAKER.name().fullName();
    }

    public static String randomPassword() {
        return "Test@" + FAKER.number().digits(4) + "Aa";
    }

    public static String weakPassword() {
        return FAKER.number().digits(3);  // Too short, no uppercase
    }

    // ── Book Data ────────────────────────────────────────────────────────────────
    public static String randomBookTitle() {
        return FAKER.book().title() + " " + FAKER.number().digits(4);
    }

    public static String randomISBN() {
        // Generate a valid-looking ISBN-13 (978-XXXXXXXXX)
        return "978" + FAKER.number().digits(10);
    }

    public static BigDecimal randomPrice() {
        double price = ThreadLocalRandom.current().nextDouble(5.0, 99.99);
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
    }

    public static int randomStock() {
        return ThreadLocalRandom.current().nextInt(1, 200);
    }

    public static String randomAuthorBio() {
        return FAKER.lorem().paragraph(2);
    }

    public static String randomDescription() {
        return FAKER.lorem().paragraph(3);
    }

    // ── Order Data ───────────────────────────────────────────────────────────────
    public static String randomOrderNote() {
        return "Test order - " + FAKER.lorem().sentence();
    }

    public static int randomQuantity() {
        return ThreadLocalRandom.current().nextInt(1, 5);
    }

    // ── Security Test Data ───────────────────────────────────────────────────────
    public static String sqlInjectionPayload() {
        String[] payloads = {
            "' OR '1'='1",
            "'; DROP TABLE users; --",
            "' OR 1=1 --",
            "1' OR '1'='1' /*"
        };
        return payloads[ThreadLocalRandom.current().nextInt(payloads.length)];
    }

    public static String xssPayload() {
        String[] payloads = {
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert(1)>",
            "javascript:alert('xss')"
        };
        return payloads[ThreadLocalRandom.current().nextInt(payloads.length)];
    }

    // ── General ──────────────────────────────────────────────────────────────────
    public static String randomString(int length) {
        return FAKER.lorem().characters(length);
    }

    public static String randomCategoryName() {
        return FAKER.book().genre() + " " + FAKER.number().digits(4);
    }
}
