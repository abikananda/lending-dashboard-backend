package com.techconsulting.lending.controller;

import com.techconsulting.lending.dto.BorrowerAnalysisResponse;
import com.techconsulting.lending.security.JwtFilter.AppPrincipal;
import com.techconsulting.lending.service.BorrowerAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/borrowers")
@RequiredArgsConstructor
public class BorrowerAnalysisController {
    private final BorrowerAnalysisService borrowerAnalysisService;

    @GetMapping("/{borrowerId}/analysis")
    public BorrowerAnalysisResponse analyse(@AuthenticationPrincipal AppPrincipal principal,
                                            @PathVariable String borrowerId) {
        return borrowerAnalysisService.analyse(principal.id(), borrowerId);
    }
}
