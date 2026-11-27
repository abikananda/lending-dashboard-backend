package com.techconsulting.lending.repository;

import com.techconsulting.lending.domain.EmailReconciliationAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailReconciliationAccountRepository extends JpaRepository<EmailReconciliationAccount, Long> {
    List<EmailReconciliationAccount> findByUserIdOrderById(Long userId);
    List<EmailReconciliationAccount> findByEnabledTrueOrderById();
    Optional<EmailReconciliationAccount> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndLenderIdIgnoreCaseAndIdNot(Long userId, String lenderId, Long id);
    boolean existsByUserIdAndBankAccountLast4AndIdNot(Long userId, String last4, Long id);
}
