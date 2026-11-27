package com.techconsulting.lending.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "email_reconciliation_account")
@Getter
@Setter
public class EmailReconciliationAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 100) private String label;
    @Column(name = "lender_id", nullable = false, length = 64) private String lenderId;
    @Column(name = "mailbox_email", nullable = false) private String mailboxEmail;
    @Column(name = "encrypted_app_password", nullable = false, length = 1000) private String encryptedAppPassword;
    @Column(name = "bank_sender", nullable = false) private String bankSender;
    @Column(name = "bank_account_last4", nullable = false, length = 4) private String bankAccountLast4;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "created_at", insertable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private Instant updatedAt;
}
