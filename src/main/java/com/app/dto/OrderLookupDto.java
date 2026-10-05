package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class OrderLookupDto {
    private String orderCode;
    private String customerName;
    private Long amount;
    private String invoiceCode;   // "N/A" if empty
    private boolean found;
}
