package com.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receipt")
@Data
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_code")
    private String receiptCode;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Column(name = "customer_name_voice")
    private String customerNameVoice;

    @Column(name = "receipt_amount")
    private Long receiptAmount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm đánh dấu đã xử lý lên Misa. null = chưa xử lý. */
    @Column(name = "misa_processed_at")
    private LocalDateTime misaProcessedAt;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReceiptOrder> orders = new ArrayList<>();

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}