package com.techconsulting.lending.repository;

import com.techconsulting.lending.domain.BankCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankCreditRepository extends JpaRepository<BankCredit, Long> {
    Optional<BankCredit> findByUserIdAndRowFingerprint(Long userId, String fingerprint);
    Optional<BankCredit> findFirstByUserIdAndBankTransactionReferenceIgnoreCase(Long userId, String reference);
    List<BankCredit> findByUserIdAndTransactionDateBetweenOrderByTransactionDateAsc(
            Long userId, LocalDate from, LocalDate to);
}
