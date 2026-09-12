package com.techconsulting.lending.service;

import com.techconsulting.lending.domain.ManualLending;
import com.techconsulting.lending.repository.ManualLendingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BorrowerAnalysisServiceTest {
    private final ManualLendingRepository repository = mock(ManualLendingRepository.class);
    private final BorrowerAnalysisService service = new BorrowerAnalysisService(repository);

    @Test
    void aggregatesRepaymentHistoryForTheLoggedInUser() {
        when(repository.findByUserIdAndBorrowerPublicIdOrderByInvestmentDateAsc(7L, "BRW-123"))
                .thenReturn(List.of(loan("L-1", "ACTIVE", false, false, "500", "150", "125", "25", "375", 3),
                        loan("L-2", "CLOSED", false, false, "500", "560", "500", "60", "0", 0)));

        var result = service.analyse(7L, " BRW-123 ");

        assertThat(result.borrowerId()).isEqualTo("BRW-123");
        assertThat(result.borrowerName()).isEqualTo("Jane Doe");
        assertThat(result.analysisResult()).isEqualTo("HEALTHY");
        assertThat(result.totalLoans()).isEqualTo(2);
        assertThat(result.totalLent()).isEqualByComparingTo("1000.00");
        assertThat(result.principalReceived()).isEqualByComparingTo("625.00");
        assertThat(result.interestReceived()).isEqualByComparingTo("85.00");
        assertThat(result.principalRepaymentPercentage()).isEqualByComparingTo("62.50");
        assertThat(result.repaymentHistory()).hasSize(2);
    }

    @Test
    void marksBorrowerHighRiskWhenAnyLoanIsNpa() {
        when(repository.findByUserIdAndBorrowerPublicIdOrderByInvestmentDateAsc(7L, "BRW-123"))
                .thenReturn(List.of(loan("L-1", "NPA", true, false, "500", "100", "100", "0", "400", 45)));

        var result = service.analyse(7L, "BRW-123");

        assertThat(result.analysisResult()).isEqualTo("HIGH_RISK");
        assertThat(result.npaLoans()).isOne();
        assertThat(result.findings()).anyMatch(value -> value.contains("classified as NPA"));
    }

    private ManualLending loan(String loanId, String status, boolean npa, boolean probableNpa,
                               String invested, String received, String principal, String interest,
                               String outstanding, int dpd) {
        ManualLending loan = new ManualLending();
        loan.setBorrowerPublicId("BRW-123");
        loan.setBorrowerName("Jane Doe");
        loan.setLoanId(loanId);
        loan.setSchemeId(loanId);
        loan.setInvestmentDate(LocalDate.of(2026, 1, 1));
        loan.setInvestedAmount(new BigDecimal(invested));
        loan.setAmountReceived(new BigDecimal(received));
        loan.setCalculatedPrincipalReceived(new BigDecimal(principal));
        loan.setCalculatedInterestReceived(new BigDecimal(interest));
        loan.setOutstandingPrincipal(new BigDecimal(outstanding));
        loan.setLoanStatus(status);
        loan.setNpa(npa);
        loan.setProbableNpa(probableNpa);
        loan.setDpd(dpd);
        return loan;
    }
}
