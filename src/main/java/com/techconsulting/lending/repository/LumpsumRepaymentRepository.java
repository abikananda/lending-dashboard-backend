package com.techconsulting.lending.repository;

import com.techconsulting.lending.domain.LumpsumRepayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LumpsumRepaymentRepository extends JpaRepository<LumpsumRepayment, Long> {
    Optional<LumpsumRepayment> findByPaymentNotificationId(Long paymentNotificationId);
}
