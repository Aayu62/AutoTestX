package com.autotestx.scripts;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * One-time script to generate the Excel DDT test data file.
 * Run: java -cp ... com.autotestx.scripts.GenerateExcelTestData
 *
 * Generates: src/test/resources/testdata/excel/login_test_data.xlsx
 */
public class GenerateExcelTestData {

    public static void main(String[] args) throws IOException {
        String outputPath = "src/test/resources/testdata/excel/login_test_data.xlsx";

        try (Workbook workbook = new XSSFWorkbook()) {
            // ── Sheet 1: LoginTests ──────────────────────────────────────────
            Sheet loginSheet = workbook.createSheet("LoginTests");

            // Header row (styling)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = loginSheet.createRow(0);
            String[] columns = {"email", "password", "expectedStatus", "scenario"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows: [email, password, expectedStatus, scenario]
            Object[][] loginData = {
                // Valid credentials
                {"user@bookverse.com",  "Admin@123", 200, "Valid user credentials"},
                {"admin@bookverse.com", "Admin@123", 200, "Valid admin credentials"},

                // Invalid password
                {"user@bookverse.com", "WrongPass1", 401, "Wrong password"},
                {"user@bookverse.com", "12345678",   401, "Simple wrong password"},

                // Non-existent user
                {"nouser@test.com",     "Test@1234", 401, "Non-existent email"},
                {"ghost@nowhere.com",   "Test@1234", 401, "Ghost user"},

                // Invalid email format → 400
                {"notanemail",          "Test@1234", 400, "Invalid email format"},
                {"@missing.com",        "Test@1234", 400, "Missing local part"},

                // Blank fields → 400
                {"",                    "Test@1234", 400, "Blank email"},
                {"user@bookverse.com",  "",          400, "Blank password"},

                // SQL injection → 400 or 401 (not 200 or 500)
                {"' OR '1'='1",         "Test@1234", 401, "SQL injection email - basic"},
                {"admin'--",            "Test@1234", 401, "SQL injection email - comment"},

                // XSS payloads
                {"<script>alert(1)</script>@test.com", "Test@1234", 400, "XSS in email"},

                // Case sensitivity
                {"USER@BOOKVERSE.COM",  "Admin@123", 401, "Uppercase email (case-sensitive)"},
                {"User@Bookverse.Com",  "Admin@123", 401, "Mixed case email"},
            };

            for (int i = 0; i < loginData.length; i++) {
                Row row = loginSheet.createRow(i + 1);
                row.createCell(0).setCellValue((String) loginData[i][0]);
                row.createCell(1).setCellValue((String) loginData[i][1]);
                row.createCell(2).setCellValue((int) loginData[i][2]);
                row.createCell(3).setCellValue((String) loginData[i][3]);
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                loginSheet.autoSizeColumn(i);
            }

            // ── Sheet 2: RegisterTests ───────────────────────────────────────
            Sheet registerSheet = workbook.createSheet("RegisterTests");
            Row rHeader = registerSheet.createRow(0);
            String[] rCols = {"name", "email", "password", "expectedStatus", "scenario"};
            for (int i = 0; i < rCols.length; i++) {
                rHeader.createCell(i).setCellValue(rCols[i]);
            }

            Object[][] registerData = {
                {"", "test@test.com",    "Test@1234", 400, "Empty name"},
                {"A", "test2@test.com",  "Test@1234", 400, "Name too short (1 char)"},
                {"Valid", "bademail",    "Test@1234", 400, "Invalid email format"},
                {"Valid", "test@ok.com", "abc",       400, "Password too short"},
                {"Valid", "test@ok.com", "nouppercase1", 400, "No uppercase in password"},
                {"Valid", "test@ok.com", "NOLOWERCASE1@", 400, "No lowercase in password"},
            };

            for (int i = 0; i < registerData.length; i++) {
                Row row = registerSheet.createRow(i + 1);
                for (int j = 0; j < registerData[i].length; j++) {
                    if (registerData[i][j] instanceof Integer) {
                        row.createCell(j).setCellValue((int) registerData[i][j]);
                    } else {
                        row.createCell(j).setCellValue((String) registerData[i][j]);
                    }
                }
            }

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
            System.out.println("✅ Excel test data generated: " + outputPath);
            System.out.println("   Sheets: LoginTests (" + loginData.length + " rows), RegisterTests (" + registerData.length + " rows)");
        }
    }
}
