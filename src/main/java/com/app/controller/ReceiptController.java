package com.app.controller;

import com.app.dto.ValidationResult;
import com.app.entity.Receipt;
import com.app.entity.ReceiptOrder;
import com.app.repository.ReceiptRepository;
import com.app.service.ReceiptValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptRepository repo;
    private final ReceiptValidationService validator;

    public ReceiptController(ReceiptRepository r, ReceiptValidationService v) {
        this.repo = r;
        this.validator = v;
    }

    /** Pre-save validation for the frontend to show yellow/red banners. */
    @PostMapping("/validate")
    public ValidationResult validate(@RequestBody Map<String, String> body) {
        String code = body.get("receiptCode");
        String dateStr = body.get("receiptDate");
        LocalDate date = (dateStr == null || dateStr.isBlank()) ? null : LocalDate.parse(dateStr);
        return validator.validate(code, date);
    }

    /** Save. Blocks duplicates server-side (returns 409). Allows not_continuous. */
    @PostMapping
    public ResponseEntity<?> save(@RequestBody Receipt receipt) {
        ValidationResult v = validator.validate(receipt.getReceiptCode(), receipt.getReceiptDate());
        if ("duplicate".equals(v.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(v);
        }
        if (receipt.getOrders() != null) {
            for (ReceiptOrder o : receipt.getOrders()) o.setReceipt(receipt);
        }
        Receipt saved = repo.save(receipt);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public List<Receipt> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Đánh dấu phiếu thu là đã xử lý lên Misa.
     * Body optional: { "processed": false } để bỏ đánh dấu.
     */
    @PatchMapping("/{id}/misa")
    public ResponseEntity<Receipt> markMisa(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Receipt r = repo.findById(id).orElse(null);
        if (r == null) return ResponseEntity.notFound().build();

        boolean processed = true;
        if (body != null && body.get("processed") instanceof Boolean b) {
            processed = b;
        }
        r.setMisaProcessedAt(processed ? LocalDateTime.now() : null);
        return ResponseEntity.ok(repo.save(r));
    }
}