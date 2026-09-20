package com.techconsulting.lending.repository;

import com.techconsulting.lending.domain.PaymentNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentNotificationRepository extends JpaRepository<PaymentNotification, Long> {
    Optional<PaymentNotification> findByUserIdAndEmailMessageId(Long userId, String emailMessageId);
    List<PaymentNotification> findByUserIdAndNotificationTypeAndNotificationDateBetweenOrderByNotificationDateDesc(
            Long userId, String type, LocalDate from, LocalDate to);
}
