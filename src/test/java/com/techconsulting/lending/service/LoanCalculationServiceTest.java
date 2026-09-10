package com.techconsulting.lending.service;

import com.techconsulting.lending.domain.LoanReportStaging;
import com.techconsulting.lending.domain.ManualLending;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;

class LoanCalculationServiceTest {
    private LoanReportStaging row() {
        var row = new LoanReportStaging();
        row.setSchemeId("L1"); row.setInvestedAmount(new BigDecimal("1000"));
        row.setAmountReceived(new BigDecimal("450")); row.setTenure(new BigDecimal("4"));
        row.setInvestmentDate(LocalDate.of(2026, 8, 1)); row.setLoanStatus("ACTIVE"); row.setDpd(0);
        return row;
    }

    @Test void separatesPrincipalAndInterest() {
        var out = service().calculate(1L, 1L, row(), new ManualLending());
        assertThat(out.getPrincipalEmi()).isEqualByComparingTo("250.00");
        assertThat(out.getCalculatedPrincipalReceived()).isEqualByComparingTo("250.00");
        assertThat(out.getCalculatedInterestReceived()).isEqualByComparingTo("200.00");
        assertThat(out.getOutstandingPrincipal()).isEqualByComparingTo("750.00");
    }

    @Test void usesReportedPrincipalAndInterestWhenAvailable() {
        var row = row(); row.setReportedPrincipalReceived(new BigDecimal("400")); row.setReportedInterestReceived(new BigDecimal("50"));
        var out = service().calculate(1L, 1L, row, new ManualLending());
        assertThat(out.getCalculatedPrincipalReceived()).isEqualByComparingTo("400.00");
        assertThat(out.getCalculatedInterestReceived()).isEqualByComparingTo("50.00");
        assertThat(out.getOutstandingPrincipal()).isEqualByComparingTo("600.00");
    }

    @Test void positiveReportedNpaAmountMarksLoanAsNpa() {
        var row = row(); row.setReportedPrincipalReceived(new BigDecimal("400")); row.setReportedNpaAmount(new BigDecimal("600"));
        var out = service().calculate(1L, 1L, row, new ManualLending());
        assertThat(out.isNpa()).isTrue(); assertThat(out.getNpaReason()).isEqualTo("REPORTED_AS_NPA");
        assertThat(out.getOutstandingPrincipal()).isEqualByComparingTo("600.00");
    }

    @Test void zeroReportedNpaAmountOverridesInferredNpa() {
        var row = row(); row.setLoanStatus("NPA"); row.setReportedNpaAmount(BigDecimal.ZERO);
        assertThat(service().calculate(1L, 1L, row, new ManualLending()).isNpa()).isFalse();
    }

    private LoanCalculationService service() {
        return new LoanCalculationService(Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC), false);
    }
}
