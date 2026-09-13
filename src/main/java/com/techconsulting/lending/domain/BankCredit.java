package com.techconsulting.lending.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bank_credit", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "row_fingerprint"}))
@Getter
@Setter
public class BankCredit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "bank_transaction_reference", length = 160) private String bankTransactionReference;
    @Column(name = "row_fingerprint", nullable = false, length = 64) private String rowFingerprint;
    @Column(name = "transaction_date", nullable = false) private LocalDate transactionDate;
    @Column(nullable = false) private BigDecimal amount;
    @Column(length = 1000) private String description;
    @Column(length = 60) private String source;
}
