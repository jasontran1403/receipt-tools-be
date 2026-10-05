package com.app.service;

import com.app.dto.OrderLookupDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ExcelLookupService {

    private final ResourceLoader resourceLoader;

    @Value("${excel.file-path}")      private String filePath;
    @Value("${excel.sheet-index}")    private int sheetIndex;
    @Value("${excel.header-row}")     private int headerRow;
    @Value("${excel.order-code-col}") private int orderCodeCol;
    @Value("${excel.customer-name-col}") private int customerNameCol;
    @Value("${excel.amount-col}")     private int amountCol;
    @Value("${excel.invoice-col}")    private int invoiceCol;

    public ExcelLookupService(ResourceLoader rl) { this.resourceLoader = rl; }

    /**
     * Look up a single order code in the Excel file.
     * Returns found=false with the code echoed back if not present.
     * Reads the file every call so users can update it live.
     */
    public OrderLookupDto lookup(String orderCode) {
        String key = normalize(orderCode);
        Resource res = resourceLoader.getResource(filePath);
        try (InputStream is = res.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(sheetIndex);
            DataFormatter fmt = new DataFormatter();
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();

            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String code = getString(row.getCell(orderCodeCol), fmt, eval);
                if (code == null) continue;
                if (normalize(code).equalsIgnoreCase(key)) {
                    String customer = getString(row.getCell(customerNameCol), fmt, eval);
                    Long amount = getLong(row.getCell(amountCol), eval);
                    String invoice = getString(row.getCell(invoiceCol), fmt, eval);
                    if (invoice == null || invoice.isBlank()) invoice = "N/A";
                    return new OrderLookupDto(orderCode, customer, amount, invoice, true);
                }
            }
        } catch (Exception e) {
            log.error("Excel read error", e);
        }
        return new OrderLookupDto(orderCode, null, null, "N/A", false);
    }

    /** Bulk lookup - opens Excel once, scans for all requested codes. */
    public Map<String, OrderLookupDto> lookupMany(java.util.List<String> codes) {
        Map<String, OrderLookupDto> out = new HashMap<>();
        Map<String, String> want = new HashMap<>();  // normalized -> original
        for (String c : codes) { out.put(c, new OrderLookupDto(c, null, null, "N/A", false)); want.put(normalize(c), c); }

        Resource res = resourceLoader.getResource(filePath);
        try (InputStream is = res.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(sheetIndex);
            DataFormatter fmt = new DataFormatter();
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();

            for (int r = headerRow + 1; r <= sheet.getLastRowNum() && !want.isEmpty(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String code = getString(row.getCell(orderCodeCol), fmt, eval);
                if (code == null) continue;
                String norm = normalize(code);
                String orig = want.remove(norm);
                if (orig != null) {
                    String customer = getString(row.getCell(customerNameCol), fmt, eval);
                    Long amount = getLong(row.getCell(amountCol), eval);
                    String invoice = getString(row.getCell(invoiceCol), fmt, eval);
                    if (invoice == null || invoice.isBlank()) invoice = "N/A";
                    out.put(orig, new OrderLookupDto(orig, customer, amount, invoice, true));
                }
            }
        } catch (Exception e) {
            log.error("Excel read error", e);
        }
        return out;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().replace(" ", "").replace(".", "").toUpperCase();
    }

    private String getString(Cell c, DataFormatter fmt, FormulaEvaluator eval) {
        if (c == null) return null;
        try {
            String v = fmt.formatCellValue(c, eval);
            return v == null ? null : v.trim();
        } catch (Exception e) { return null; }
    }

    private Long getLong(Cell c, FormulaEvaluator eval) {
        if (c == null) return null;
        try {
            CellType t = c.getCellType() == CellType.FORMULA ? eval.evaluate(c).getCellType() : c.getCellType();
            if (t == CellType.NUMERIC) {
                double d = c.getCellType() == CellType.FORMULA ? eval.evaluate(c).getNumberValue() : c.getNumericCellValue();
                return Math.round(d);
            }
            if (t == CellType.STRING) {
                String s = c.getStringCellValue().replaceAll("[^0-9-]", "");
                return s.isEmpty() ? null : Long.parseLong(s);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
