package com.app.service;

import com.app.dto.OrderLookupDto;
import com.app.entity.InvoiceRecord;
import com.app.repository.InvoiceRecordRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceLookupService {

    private final InvoiceRecordRepository repo;

    public InvoiceLookupService(InvoiceRecordRepository repo) {
        this.repo = repo;
    }

    /** Single lookup, O(log n) nhờ index trên order_code_key. */
    public OrderLookupDto lookup(String orderCode) {
        if (orderCode == null) return notFound(null);
        String key = InvoiceImportService.normalizeKey(orderCode);
        return repo.findFirstByOrderCodeKey(key)
                .map(r -> new OrderLookupDto(
                        orderCode,
                        r.getCustomerName(),
                        r.getAmount(),
                        r.getInvoiceCode() == null ? "N/A" : r.getInvoiceCode(),
                        true
                ))
                .orElseGet(() -> notFound(orderCode));
    }

    /** Bulk lookup — 1 query duy nhất cho N codes nhờ IN(...) + index. */
    public Map<String, OrderLookupDto> lookupMany(List<String> codes) {
        Map<String, OrderLookupDto> out = new HashMap<>();
        if (codes == null || codes.isEmpty()) return out;

        // Giữ mapping normalized -> original (user có thể gửi "3121" hoặc " 3121 ")
        Map<String, String> keyToOriginal = new HashMap<>();
        for (String c : codes) {
            if (c == null) continue;
            String k = InvoiceImportService.normalizeKey(c);
            keyToOriginal.putIfAbsent(k, c);
            out.put(c, notFound(c)); // default N/A
        }
        if (keyToOriginal.isEmpty()) return out;

        List<InvoiceRecord> found = repo.findByOrderCodeKeyIn(keyToOriginal.keySet());
        for (InvoiceRecord r : found) {
            String orig = keyToOriginal.get(r.getOrderCodeKey());
            if (orig == null) continue;
            out.put(orig, new OrderLookupDto(
                    orig,
                    r.getCustomerName(),
                    r.getAmount(),
                    r.getInvoiceCode() == null ? "N/A" : r.getInvoiceCode(),
                    true
            ));
        }
        return out;
    }

    private OrderLookupDto notFound(String code) {
        return new OrderLookupDto(code, null, null, "N/A", false);
    }
}