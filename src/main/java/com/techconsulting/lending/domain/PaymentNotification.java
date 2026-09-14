package com.techconsulting.lending.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "payment_notification", uniqueConstraints =
        @UniqueConstraint(columnNames = {"user_id", "email_message_id"}))
@Getter
@Setter
public class PaymentNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "reconciliation_account_id") private Long reconciliationAccountId;
    @Column(name = "email_message_id", nullable = false, length = 255) private String emailMessageId;
    @Column(name = "email_received_at", nullable = false) private Instant emailReceivedAt;
    @Column(length = 255) private String sender;
    @Column(length = 500) private String subject;
    @Column(name = "notification_date") private LocalDate notificationDate;
    @Column(name = "reported_amount") private BigDecimal reportedAmount;
    @Column(name = "principal_amount") private BigDecimal principalAmount;
    @Column(name = "interest_amount") private BigDecimal interestAmount;
    @Column(name = "lending_type", length = 50) private String lendingType;
    @Column(name = "account_last4", length = 4) private String accountLast4;
    @Column(name = "transaction_reference", length = 160) private String transactionReference;
    @Column(name = "amount_validation_status", length = 30) private String amountValidationStatus;
    @Column(name = "notification_type", nullable = false, length = 40) private String notificationType;
    @Column(name = "raw_email_text", columnDefinition = "text") private String rawEmailText;
    @Column(name = "parsing_status", nullable = false, length = 30) private String parsingStatus;
    @Column(name = "parsing_error", columnDefinition = "text") private String parsingError;
}
