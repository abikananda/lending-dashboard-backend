package com.techconsulting.lending.service;

import com.techconsulting.lending.dto.DashboardSummary;

import java.math.BigDecimal;
import java.util.List;

public record ManualUploadNotification(Long batchId, String filename, String recipient,
        DashboardSummary before, DashboardSummary after, List<NewNpaBorrower> newNpaBorrowers) {
    public ManualUploadNotification {
        newNpaBorrowers = List.copyOf(newNpaBorrowers);
    }

    public record NewNpaBorrower(String borrowerName, String loanId, String schemeId,
            BigDecimal investedAmount, BigDecimal principalReceived, BigDecimal npaAmount, String reason) { }
}
