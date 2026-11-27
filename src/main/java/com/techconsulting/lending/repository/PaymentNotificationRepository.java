package com.techconsulting.lending.repository;

import com.techconsulting.lending.domain.PaymentNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentNotificationRepository extends JpaRepository<PaymentNotification, Long> {
    Optional<PaymentNotification> findByUserIdAndEmailMessageId(Long userId, String emailMessageId);
    List<PaymentNotification> findByUserIdAndNotificationTypeAndNotificationDateBetweenOrderByNotificationDateDesc(
            Long userId, String type, LocalDate from, LocalDate to);

    @Query("""
            select notification from PaymentNotification notification
            where notification.userId = :userId
              and notification.notificationType = 'LENDENCLUB_REPAYMENT'
              and not exists (
                  select repayment.id from LumpsumRepayment repayment
                  where repayment.paymentNotificationId = notification.id
              )
            order by notification.notificationDate desc
            """)
    List<PaymentNotification> findStoredLendenclubEmailsMissingLumpsumRepayment(@Param("userId") Long userId);
}
