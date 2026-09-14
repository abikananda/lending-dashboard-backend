package com.techconsulting.lending.controller;

import com.techconsulting.lending.domain.ReconciliationRecord;
import com.techconsulting.lending.security.JwtFilter.AppPrincipal;
import com.techconsulting.lending.service.EmailReconciliationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/email-reconciliation")
public class EmailReconciliationController {
    private final EmailReconciliationService service;

    public EmailReconciliationController(EmailReconciliationService service) { this.service = service; }

    @PostMapping("/sync")
    public EmailReconciliationService.SyncResult sync(@AuthenticationPrincipal AppPrincipal principal) {
        return service.sync(principal.id());
    }

    @GetMapping
    public List<ReconciliationRecord> records(
            @AuthenticationPrincipal AppPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        if (start.isAfter(end)) throw new IllegalArgumentException("from cannot be after to");
        return service.records(principal.id(), start, end);
    }
}
