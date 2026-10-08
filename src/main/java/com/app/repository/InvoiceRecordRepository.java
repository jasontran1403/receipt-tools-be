package com.app.repository;

import com.app.entity.InvoiceRecord;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvoiceRecordRepository extends JpaRepository<InvoiceRecord, Long> {

    /** Lookup chính theo mã đơn (đã normalize). O(log n) nhờ index. */
    Optional<InvoiceRecord> findFirstByOrderCodeKey(String orderCodeKey);

    /** Bulk lookup. */
    List<InvoiceRecord> findByOrderCodeKeyIn(Collection<String> keys);

    /** Lookup theo invoice (F.Inv). */
    List<InvoiceRecord> findByInvoiceCode(String invoiceCode);

    /** Xoá toàn bộ nhanh hơn deleteAllInBatch khi reimport. */
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE invoice_record", nativeQuery = true)
    void truncate();
}