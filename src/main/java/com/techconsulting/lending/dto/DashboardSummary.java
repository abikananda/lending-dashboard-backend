package com.techconsulting.lending.dto;

import java.math.BigDecimal;

public record DashboardSummary(
        BigDecimal investmentPrincipal, BigDecimal totalAmountLent, BigDecimal totalAmountReceived,
        BigDecimal interestEarned, BigDecimal interestPercentage, BigDecimal outstandingPrincipal,
        BigDecimal amountAvailableToInvest, BigDecimal principalLoss, BigDecimal principalLossPercentage,
        BigDecimal walletAdded, BigDecimal walletWithdrawn, BigDecimal bankReceived,
        long activeLoans, long closedLoans, long npaLoans, BigDecimal npaAmount, BigDecimal npaPercentage,
        long probableNpaLoans, BigDecimal probableNpaAmount, BigDecimal probableNpaPercentage,
        String portfolioHealth) { }
