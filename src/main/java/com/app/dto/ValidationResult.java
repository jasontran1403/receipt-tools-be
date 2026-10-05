package com.app.dto;

public record ValidationResult(
        String status,          // "ok" | "not_continuous" | "duplicate"
        String message,
        String expectedCode,    // for not_continuous
        String duplicateDate    // for duplicate (ISO date)
) {
    public static ValidationResult ok() {
        return new ValidationResult("ok", null, null, null);
    }
    public static ValidationResult notContinuous(String expected, String lastCode) {
        return new ValidationResult(
                "not_continuous",
                "Không liên tục — phiếu thu gần nhất là " + lastCode + ", số kế tiếp nên là " + expected,
                expected, null
        );
    }
    public static ValidationResult duplicate(String date) {
        return new ValidationResult(
                "duplicate",
                "Phiếu thu này đã tồn tại vào ngày " + date + " (cách dưới 6 tháng).",
                null, date
        );
    }
}