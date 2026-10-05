package com.app.repository;

import com.app.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findAllByOrderByCreatedAtDesc();

    /** Latest inserted receipt, used for continuity check. */
    Optional<Receipt> findTopByOrderByCreatedAtDesc();

    /** All receipts with the same code — used to detect same-code within 6 months. */
    List<Receipt> findByReceiptCode(String receiptCode);
}