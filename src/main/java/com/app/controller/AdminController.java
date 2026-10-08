package com.app.controller;

import com.app.repository.InvoiceRecordRepository;
import com.app.service.InvoiceImportService;
import com.app.service.InvoiceImportService.ImportResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint quản trị — gọi khi file Excel invoice.xlsx được cập nhật.
 *
 *   POST /api/admin/reimport  → truncate + reimport toàn bộ
 *   GET  /api/admin/invoice-count  → đếm số dòng hiện tại
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final InvoiceImportService importService;
    private final InvoiceRecordRepository repo;

    public AdminController(InvoiceImportService imp, InvoiceRecordRepository repo) {
        this.importService = imp;
        this.repo = repo;
    }

    @PostMapping("/reimport")
    public ImportResult reimport() {
        return importService.importAll();
    }

    @GetMapping("/invoice-count")
    public Map<String, Long> count() {
        return Map.of("count", repo.count());
    }
}