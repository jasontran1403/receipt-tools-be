package com.app.service;

import com.app.entity.InvoiceRecord;
import com.app.repository.InvoiceRecordRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class InvoiceImportService {

    private static final int BATCH_SIZE = 500;

    private final InvoiceRecordRepository repo;
    private final ResourceLoader resourceLoader;
    private final TransactionTemplate tx;

    @PersistenceContext
    private EntityManager em;

    @Value("${excel.file-path}")         private String filePath;
    @Value("${excel.sheet-index}")       private int sheetIndex;
    @Value("${excel.header-row}")        private int headerRow;
    @Value("${excel.order-code-col}")    private int orderCodeCol;
    @Value("${excel.customer-name-col}") private int customerNameCol;
    @Value("${excel.amount-col}")        private int amountCol;
    @Value("${excel.invoice-col}")       private int invoiceCol;

    public InvoiceImportService(
            InvoiceRecordRepository repo,
            ResourceLoader rl,
            PlatformTransactionManager txManager
    ) {
        this.repo = repo;
        this.resourceLoader = rl;
        this.tx = new TransactionTemplate(txManager);
        // Timeout rộng vì import 25K+ rows
        this.tx.setTimeout(300);
    }

    /** Import tự động khi app khởi động nếu DB rỗng. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        long count = repo.count();
        if (count == 0) {
            log.info("invoice_record is empty — auto importing from Excel...");
            try {
                ImportResult r = importAll();
                log.info("Auto-import done: {}", r);
            } catch (Exception e) {
                log.error("Auto-import failed", e);
            }
        } else {
            log.info("invoice_record has {} rows — skipping auto-import.", count);
        }
    }

    /**
     * Reimport toàn bộ: truncate rồi insert lại.
     * Dùng TransactionTemplate programmatic → không phụ thuộc Spring proxy,
     * nên gọi được từ cả @EventListener (self-invocation) lẫn Controller.
     */
    public ImportResult importAll() {
        return tx.execute(status -> doImport());
    }

    private ImportResult doImport() {
        long batchId = System.currentTimeMillis();
        long t0 = batchId;

        repo.truncate();

        Resource res = resourceLoader.getResource(filePath);
        int ok = 0, skipped = 0;
        List<InvoiceRecord> batch = new ArrayList<>(BATCH_SIZE);

        try (InputStream is = res.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(sheetIndex);
            DataFormatter fmt = new DataFormatter();
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();

            int lastRow = sheet.getLastRowNum();
            for (int r = headerRow + 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) { skipped++; continue; }

                String code = getString(row.getCell(orderCodeCol), fmt, eval);
                if (code == null || code.isBlank()) { skipped++; continue; }

                InvoiceRecord rec = new InvoiceRecord();
                rec.setOrderCode(code);
                rec.setOrderCodeKey(normalize(code));
                rec.setCustomerName(getString(row.getCell(customerNameCol), fmt, eval));
                rec.setAmount(getLong(row.getCell(amountCol), eval));
                String inv = getString(row.getCell(invoiceCol), fmt, eval);
                rec.setInvoiceCode(inv == null || inv.isBlank() ? null : inv);
                rec.setImportBatch(batchId);

                batch.add(rec);
                if (batch.size() >= BATCH_SIZE) {
                    repo.saveAll(batch);
                    em.flush();
                    em.clear();
                    batch.clear();
                }
                ok++;
            }
            if (!batch.isEmpty()) {
                repo.saveAll(batch);
                em.flush();
                em.clear();
            }
        } catch (Exception e) {
            log.error("Import failed", e);
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }

        long ms = System.currentTimeMillis() - t0;
        return new ImportResult(batchId, ok, skipped, ms);
    }

    // ----- Excel cell helpers -----

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
                double d = c.getCellType() == CellType.FORMULA
                        ? eval.evaluate(c).getNumberValue()
                        : c.getNumericCellValue();
                return Math.round(d);
            }
            if (t == CellType.STRING) {
                String s = c.getStringCellValue().replaceAll("[^0-9-]", "");
                return s.isEmpty() ? null : Long.parseLong(s);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Normalize dùng chung với LookupService (public static helper). */
    public static String normalizeKey(String s) {
        if (s == null) return "";
        return s.trim().replace(" ", "").replace(".", "").toUpperCase();
    }

    public record ImportResult(long batchId, int inserted, int skipped, long elapsedMs) {}
}
