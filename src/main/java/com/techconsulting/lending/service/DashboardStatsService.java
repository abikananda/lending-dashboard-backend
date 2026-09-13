package com.techconsulting.lending.service;

import com.techconsulting.lending.domain.LendingPortfolio;
import com.techconsulting.lending.dto.DashboardSummary;
import com.techconsulting.lending.repository.LendingPortfolioRepository;
import com.techconsulting.lending.repository.ManualLendingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {
    private final ManualLendingRepository loans;
    private final LendingPortfolioRepository portfolios;

    public DashboardSummary snapshot(Long userId) {
        BigDecimal principal=portfolios.findByUserId(userId).map(LendingPortfolio::getInvestmentPrincipalAmount)
                .orElse(BigDecimal.ZERO);
        BigDecimal interest=loans.totalInterestEarned(userId), outstanding=loans.totalOutstanding(userId);
        BigDecimal loss=loans.totalPrincipalLoss(userId), npaAmount=loans.totalNpaAmount(userId);
        BigDecimal probableNpaAmount=loans.totalProbableNpaAmount(userId);
        BigDecimal npaPercentage=percent(npaAmount,principal);
        BigDecimal probableNpaPercentage=percent(probableNpaAmount,principal);
        BigDecimal available=principal.add(interest).subtract(outstanding).max(BigDecimal.ZERO);
        return new DashboardSummary(principal,loans.totalInvested(userId),loans.totalReceived(userId),interest,
                percent(interest,principal),outstanding,available,loss,percent(loss,principal),
                loans.totalWalletAdded(userId),loans.totalWalletWithdrawn(userId),loans.totalBankReceived(userId),
                loans.countStatus(userId,"ACTIVE"),loans.countStatus(userId,"CLOSED"),loans.countNpa(userId),
                npaAmount,npaPercentage,loans.countProbableNpa(userId),probableNpaAmount,probableNpaPercentage,
                portfolioHealth(npaPercentage,probableNpaPercentage));
    }

    private BigDecimal percent(BigDecimal amount,BigDecimal principal) {
        return principal.signum()==0 ? BigDecimal.ZERO
                : amount.multiply(BigDecimal.valueOf(100)).divide(principal,4,RoundingMode.HALF_UP);
    }

    static String portfolioHealth(BigDecimal npaPercentage,BigDecimal probableNpaPercentage) {
        if(npaPercentage.compareTo(BigDecimal.valueOf(20))>0||probableNpaPercentage.compareTo(BigDecimal.valueOf(40))>0)
            return "Critical";
        if(npaPercentage.compareTo(BigDecimal.valueOf(15))>0||probableNpaPercentage.compareTo(BigDecimal.valueOf(30))>0)
            return "Needs attention";
        return "Healthy";
    }
}
