package com.app.service;

import com.app.dto.ValidationResult;
import com.app.entity.Receipt;
import com.app.repository.ReceiptRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class ReceiptValidationService {

    private static final long MIN_MONTHS_FOR_REUSE = 6;

    private final ReceiptRepository repo;
    public ReceiptValidationService(ReceiptRepository r) { this.repo = r; }

    public ValidationResult validate(String receiptCode, LocalDate receiptDate) {
        if (receiptCode == null || receiptCode.isBlank()) {
            return ValidationResult.ok(); // nothing to check
        }

        // ---- Duplicate check (higher priority) ----
        List<Receipt> sameCode = repo.findByReceiptCode(receiptCode);
        if (receiptDate != null) {
            for (Receipt r : sameCode) {
                LocalDate d = r.getReceiptDate();
                if (d == null) continue;
                long months = Math.abs(ChronoUnit.MONTHS.between(d, receiptDate));
                if (months < MIN_MONTHS_FOR_REUSE) {
                    return ValidationResult.duplicate(d.toString());
                }
            }
        } else if (!sameCode.isEmpty()) {
            // No date to compare — treat as duplicate for safety
            return ValidationResult.duplicate(sameCode.get(0).getReceiptDate() != null
                    ? sameCode.get(0).getReceiptDate().toString() : "?");
        }

        // ---- Continuity check ----
        Optional<Receipt> latest = repo.findTopByOrderByCreatedAtDesc();
        if (latest.isPresent()) {
            String lastCode = latest.get().getReceiptCode();
            try {
                long lastNum = Long.parseLong(lastCode);
                long curNum = Long.parseLong(receiptCode);
                if (curNum != lastNum + 1) {
                    return ValidationResult.notContinuous(String.valueOf(lastNum + 1), lastCode);
                }
            } catch (NumberFormatException ignored) {
                // Non-numeric codes: skip continuity check
            }
        }

        return ValidationResult.ok();
    }
}