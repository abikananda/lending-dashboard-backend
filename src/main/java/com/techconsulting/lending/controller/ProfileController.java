package com.techconsulting.lending.controller;

import com.techconsulting.lending.domain.User;
import com.techconsulting.lending.repository.UserRepository;
import com.techconsulting.lending.security.JwtFilter.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final UserRepository users;

    public record LenderIdRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9]+") String lenderId) { }
    public record LenderIdResponse(String lenderId) { }

    @PutMapping("/lender-id")
    LenderIdResponse updateLenderId(@AuthenticationPrincipal AppPrincipal principal,
                                    @Valid @RequestBody LenderIdRequest request) {
        String lenderId = request.lenderId().trim().toUpperCase();
        users.findByLenderIdIgnoreCase(lenderId)
                .filter(owner -> !owner.getId().equals(principal.id()))
                .ifPresent(owner -> { throw new IllegalArgumentException("Lender ID is already registered"); });
        User user = users.findById(principal.id()).orElseThrow();
        user.setLenderId(lenderId);
        users.save(user);
        return new LenderIdResponse(lenderId);
    }
}
