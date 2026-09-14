package com.techconsulting.lending.service;

import com.techconsulting.lending.domain.ManualLending;
import com.techconsulting.lending.dto.BorrowerAnalysisResponse;
import com.techconsulting.lending.repository.ManualLendingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class BorrowerAnalysisService {
    private final ManualLendingRepository loans;

    @Transactional(readOnly = true)
    public BorrowerAnalysisResponse analyse(Long userId, String borrowerId) {
        String normalizedId = borrowerId == null ? "" : borrowerId.trim();
        if (normalizedId.isEmpty()) throw new IllegalArgumentException("Borrower ID is required");

        List<ManualLending> history = loans
                .findByUserIdAndBorrowerPublicIdOrderByInvestmentDateAsc(userId, normalizedId);
        if (history.isEmpty()) throw new NoSuchElementException("Borrower not found: " + normalizedId);

        BigDecimal totalLent = sum(history, ManualLending::getInvestedAmount);
        BigDecimal totalReceived = sum(history, ManualLending::getAmountReceived);
        BigDecimal principalReceived = sum(history, ManualLending::getCalculatedPrincipalReceived);
        BigDecimal interestReceived = sum(history, ManualLending::getCalculatedInterestReceived);
        BigDecimal outstanding = sum(history, ManualLending::getOutstandingPrincipal);
        long active = history.stream().filter(loan -> "ACTIVE".equalsIgnoreCase(loan.getLoanStatus())).count();
        long closed = history.stream().filter(loan -> "CLOSED".equalsIgnoreCase(loan.getLoanStatus())).count();
        long npa = history.stream().filter(ManualLending::isNpa).count();
        long probableNpa = history.stream().filter(ManualLending::isProbableNpa).count();
        int maximumDpd = history.stream().mapToInt(ManualLending::getDpd).max().orElse(0);
        BigDecimal averageDpd = BigDecimal.valueOf(history.stream().mapToInt(ManualLending::getDpd).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);

        List<String> findings = new ArrayList<>();
        String result;
        if (npa > 0) {
            result = "HIGH_RISK";
            findings.add(npa + " loan(s) are classified as NPA");
        } else if (probableNpa > 0 || maximumDpd > 7) {
            result = "NEEDS_ATTENTION";
            if (probableNpa > 0) findings.add(probableNpa + " loan(s) are probable NPA");
            if (maximumDpd > 7) findings.add("Maximum delay is " + maximumDpd + " days");
        } else {
            result = "HEALTHY";
            findings.add("No NPA or probable-NPA repayment was found");
        }
        findings.add(closed + " of " + history.size() + " loan(s) are closed");
        findings.add(percent(principalReceived, totalLent).stripTrailingZeros().toPlainString()
                + "% of lent principal has been received");

        List<BorrowerAnalysisResponse.RepaymentHistoryItem> repayments = history.stream().map(loan ->
                new BorrowerAnalysisResponse.RepaymentHistoryItem(
                        loan.getLoanId(), loan.getSchemeId(), loan.getInvestmentDate(), loan.getLastEmiPaidOn(),
                        loan.getExpectedMaturityDate(), money(loan.getInvestedAmount()), money(loan.getAmountReceived()),
                        money(loan.getCalculatedPrincipalReceived()), money(loan.getCalculatedInterestReceived()),
                        money(loan.getOutstandingPrincipal()), loan.getDpd(), loan.getLoanStatus(), loan.isNpa(),
                        loan.isProbableNpa())).toList();

        return new BorrowerAnalysisResponse(normalizedId, history.get(history.size() - 1).getBorrowerName(), result,
                findings, history.size(), active, closed, npa, probableNpa, totalLent, totalReceived,
                principalReceived, interestReceived, outstanding, percent(principalReceived, totalLent),
                percent(interestReceived, totalLent), averageDpd, maximumDpd, repayments);
    }

    private BigDecimal sum(List<ManualLending> values,
                           java.util.function.Function<ManualLending, BigDecimal> mapper) {
        return values.stream().map(mapper).map(this::money).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal amount, BigDecimal total) {
        return total.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                : amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }
}
