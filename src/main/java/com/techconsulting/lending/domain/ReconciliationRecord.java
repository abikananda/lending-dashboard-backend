package com.techconsulting.lending.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reconciliation_record")
@Getter
@Setter
public class ReconciliationRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "reconciliation_date", nullable = false) private LocalDate reconciliationDate;
    @Column(name = "lender_reported_amount") private BigDecimal lenderReportedAmount;
    @Column(name = "bank_credited_amount") private BigDecimal bankCreditedAmount;
    @Column(name = "difference_amount", nullable = false) private BigDecimal differenceAmount;
    @Column(nullable = false, length = 50) private String status;
    @Column(length = 1000) private String reason;
    @Column(name = "payment_notification_id") private Long paymentNotificationId;
    @Column(name = "bank_payment_notification_id") private Long bankPaymentNotificationId;
    @Column(name = "bank_credit_id") private Long bankCreditId;
    @Column(name = "manually_resolved", nullable = false) private boolean manuallyResolved;
}
