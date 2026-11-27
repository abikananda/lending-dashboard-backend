package com.techconsulting.lending.repository;

import com.techconsulting.lending.domain.ReconciliationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReconciliationRecordRepository extends JpaRepository<ReconciliationRecord, Long> {
    Optional<ReconciliationRecord> findByUserIdAndPaymentNotificationId(Long userId, Long notificationId);
    List<ReconciliationRecord> findByUserIdAndReconciliationDateBetweenOrderByReconciliationDateDesc(
            Long userId, LocalDate from, LocalDate to);
}
