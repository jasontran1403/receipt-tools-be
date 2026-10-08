package com.app.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Mỗi row trong file Excel invoice.xlsx được map thành 1 record ở đây.
 * Được index theo order_code_key và invoice_code để lookup nhanh.
 */
@Entity
@Table(
        name = "invoice_record",
        indexes = {
                @Index(name = "idx_invoice_order_code_key", columnList = "order_code_key"),
                @Index(name = "idx_invoice_invoice_code",   columnList = "invoice_code")
        }
)
@Data
public class InvoiceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã đơn gốc đọc từ Excel cột B (VD: "3121", "NĐ-03054"). */
    @Column(name = "order_code", length = 100)
    private String orderCode;

    /** Mã đơn đã normalize (trim, remove space/dot, uppercase). INDEXED. */
    @Column(name = "order_code_key", length = 100, nullable = false)
    private String orderCodeKey;

    /** Tên khách (cột C). */
    @Column(name = "customer_name", length = 500)
    private String customerName;

    /** Số tiền (cột D). Giữ nguyên long math như ExcelLookupService cũ. */
    @Column(name = "amount")
    private Long amount;

    /** Mã invoice F.Inv (cột E). INDEXED. */
    @Column(name = "invoice_code", length = 100)
    private String invoiceCode;

    /** Batch id của lần import cuối — tiện debug / cleanup. */
    @Column(name = "import_batch")
    private Long importBatch;
}