package com.techconsulting.lending.controller;

import com.techconsulting.lending.domain.*;
import com.techconsulting.lending.dto.DashboardSummary;
import com.techconsulting.lending.repository.*;
import com.techconsulting.lending.security.JwtFilter.AppPrincipal;
import com.techconsulting.lending.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LendingController {
    private final LoanExcelImportService importer;
    private final UploadedDataCleanupService cleanupService;
    private final ImportBatchRepository batches;
    private final LoanReportStagingRepository staging;
    private final ManualLendingRepository loans;
    private final LendingPortfolioRepository portfolios;
    private final DashboardStatsService dashboardStats;

    @PostMapping(value="/imports/loan-report",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    ImportBatch upload(@AuthenticationPrincipal AppPrincipal p,@RequestPart("file") MultipartFile file)throws Exception {
        return importer.upload(p.id(),file);
    }
    @GetMapping("/imports") List<ImportBatch> imports(@AuthenticationPrincipal AppPrincipal p) {
        return batches.findByUserIdOrderByCreatedAtDesc(p.id());
    }
    @GetMapping("/imports/{id}") ImportBatch batch(@AuthenticationPrincipal AppPrincipal p,@PathVariable Long id) {
        return batches.findById(id).filter(x->x.getUserId().equals(p.id())).orElseThrow();
    }
    @GetMapping("/imports/{id}/rows") List<LoanReportStaging> rows(
            @AuthenticationPrincipal AppPrincipal p,@PathVariable Long id) {
        batch(p,id); return staging.findByImportBatchIdOrderByRowNumber(id);
    }
    @GetMapping("/loans") List<ManualLending> loanList(@AuthenticationPrincipal AppPrincipal p) {
        return loans.findByUserIdOrderByInvestmentDateDesc(p.id());
    }

    public record PrincipalRequest(BigDecimal amount) { }

    @GetMapping("/dashboard/summary")
    DashboardSummary summary(@AuthenticationPrincipal AppPrincipal p) {
        return dashboardStats.snapshot(p.id());
    }

    @PutMapping("/dashboard/principal")
    DashboardSummary principal(@AuthenticationPrincipal AppPrincipal p,@RequestBody PrincipalRequest request) {
        if(request.amount()==null||request.amount().signum()<0)
            throw new IllegalArgumentException("Principal amount must be zero or positive");
        LendingPortfolio portfolio=portfolios.findByUserId(p.id()).orElseGet(LendingPortfolio::new);
        portfolio.setUserId(p.id()); portfolio.setInvestmentPrincipalAmount(request.amount());
        portfolios.save(portfolio);
        return dashboardStats.snapshot(p.id());
    }

    @DeleteMapping("/dashboard/uploaded-data")
    UploadedDataCleanupService.CleanupResult cleanup(@AuthenticationPrincipal AppPrincipal p) {
        return cleanupService.cleanup(p.id());
    }
}
