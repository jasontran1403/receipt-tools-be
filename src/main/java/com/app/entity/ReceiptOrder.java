package com.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "receipt_order")
@Data
public class ReceiptOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id")
    @JsonIgnore
    private Receipt receipt;

    @Column(name = "order_code")
    private String orderCode;

    @Column(name = "invoice_code")
    private String invoiceCode;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "order_amount")
    private Long orderAmount;
}
