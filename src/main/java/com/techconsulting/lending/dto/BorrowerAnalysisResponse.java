package com.techconsulting.lending.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BorrowerAnalysisResponse(
        String borrowerId,
        String borrowerName,
        String analysisResult,
        List<String> findings,
        int totalLoans,
        long activeLoans,
        long closedLoans,
        long npaLoans,
        long probableNpaLoans,
        BigDecimal totalLent,
        BigDecimal totalReceived,
        BigDecimal principalReceived,
        BigDecimal interestReceived,
        BigDecimal outstandingPrincipal,
        BigDecimal principalRepaymentPercentage,
        BigDecimal returnPercentage,
        BigDecimal averageDpd,
        int maximumDpd,
        List<RepaymentHistoryItem> repaymentHistory) {

    public record RepaymentHistoryItem(
            String loanId,
            String schemeId,
            LocalDate investmentDate,
            LocalDate lastEmiPaidOn,
            LocalDate expectedMaturityDate,
            BigDecimal amountLent,
            BigDecimal amountReceived,
            BigDecimal principalReceived,
            BigDecimal interestReceived,
            BigDecimal outstandingPrincipal,
            int dpd,
            String loanStatus,
            boolean npa,
            boolean probableNpa) { }
}
