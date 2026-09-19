package com.techconsulting.lending.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "lumpsum_repayment")
@Getter
@Setter
public class LumpsumRepayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "reconciliation_account_id") private Long reconciliationAccountId;
    @Column(name = "payment_notification_id", nullable = false, unique = true) private Long paymentNotificationId;
    @Column(name = "bank_payment_notification_id") private Long bankPaymentNotificationId;
    @Column(name = "processing_date", nullable = false) private LocalDate processingDate;
    @Column(name = "principal_amount", nullable = false) private BigDecimal principalAmount;
    @Column(name = "interest_amount", nullable = false) private BigDecimal interestAmount;
    @Column(name = "total_amount", nullable = false) private BigDecimal totalAmount;
    @Column(name = "lender_reported_amount", nullable = false) private BigDecimal lenderReportedAmount;
    @Column(name = "bank_credit_amount") private BigDecimal bankCreditAmount;
    @Column(name = "difference_amount") private BigDecimal differenceAmount;
    @Column(name = "subject_validation_status", nullable = false, length = 30) private String subjectValidationStatus;
    @Column(name = "bank_validation_status", nullable = false, length = 30) private String bankValidationStatus;
    @Column(name = "validation_error", length = 1000) private String validationError;
    @Column(name = "created_at", insertable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private Instant updatedAt;
}
