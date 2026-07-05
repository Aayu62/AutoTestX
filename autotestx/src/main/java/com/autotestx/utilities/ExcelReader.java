package com.autotestx.utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel test data reader using Apache POI.
 *
 * Reads .xlsx files from src/test/resources/testdata/excel/
 *
 * Usage (TestNG DataProvider):
 *   @DataProvider(name = "loginData")
 *   public Object[][] loginData() {
 *       return ExcelReader.readSheet("login_test_data.xlsx", "LoginTests");
 *   }
 */
public final class ExcelReader {

    private static final Logger log = LogManager.getLogger(ExcelReader.class);

    private ExcelReader() {}

    /**
     * Reads an Excel sheet and returns Object[][] for TestNG @DataProvider.
     * Row 0 is the header row (skipped). Each subsequent row is a test case.
     */
    public static Object[][] readSheet(String fileName, String sheetName) {
        String filePath = "testdata/excel/" + fileName;
        log.info("Reading Excel: {} | Sheet: {}", filePath, sheetName);

        try (InputStream is = ExcelReader.class.getClassLoader().getResourceAsStream(filePath)) {
            if (is == null) {
                throw new RuntimeException("Excel file not found: " + filePath);
            }

            try (Workbook workbook = new XSSFWorkbook(is)) {
                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    throw new RuntimeException("Sheet not found: " + sheetName + " in " + filePath);
                }

                int rowCount = sheet.getLastRowNum();
                int colCount = sheet.getRow(0).getLastCellNum();

                List<Object[]> data = new ArrayList<>();

                for (int i = 1; i <= rowCount; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || isRowEmpty(row)) continue;

                    Object[] rowData = new Object[colCount];
                    for (int j = 0; j < colCount; j++) {
                        rowData[j] = getCellValue(row.getCell(j));
                    }
                    data.add(rowData);
                }

                log.info("Read {} test rows from {}/{}", data.size(), fileName, sheetName);
                return data.toArray(new Object[0][]);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel: " + filePath, e);
        }
    }

    /**
     * Get headers from the first row of a sheet.
     */
    public static List<String> getHeaders(String fileName, String sheetName) {
        String filePath = "testdata/excel/" + fileName;
        try (InputStream is = ExcelReader.class.getClassLoader().getResourceAsStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(sheetName);
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }
            return headers;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel headers", e);
        }
    }

    private static Object getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                            ? cell.getDateCellValue().toString()
                            : cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            default      -> "";
        };
    }

    private static boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}
