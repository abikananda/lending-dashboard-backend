package com.techconsulting.lending.controller;

import com.techconsulting.lending.config.EmailReconciliationProperties;
import com.techconsulting.lending.domain.EmailReconciliationAccount;
import com.techconsulting.lending.repository.EmailReconciliationAccountRepository;
import com.techconsulting.lending.security.JwtFilter.AppPrincipal;
import com.techconsulting.lending.service.EmailCredentialCipher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profile/reconciliation-accounts")
public class EmailReconciliationAccountController {
    private final EmailReconciliationAccountRepository accounts;
    private final EmailCredentialCipher cipher;
    private final EmailReconciliationProperties properties;

    public EmailReconciliationAccountController(EmailReconciliationAccountRepository accounts,
                                                EmailCredentialCipher cipher,
                                                EmailReconciliationProperties properties) {
        this.accounts = accounts; this.cipher = cipher; this.properties = properties;
    }

    public record AccountRequest(@NotBlank @Size(max=100) String label,
            @NotBlank @Pattern(regexp="[A-Za-z0-9]+") String lenderId,
            @NotBlank @Email String mailboxEmail, String gmailAppPassword,
            @NotBlank @Email String bankSender,
            @NotBlank @Pattern(regexp="\\d{4}") String bankAccountLast4, boolean enabled) { }
    public record AccountResponse(Long id, String label, String lenderId, String mailboxEmail,
            String bankSender, String bankAccountLast4, boolean enabled, boolean credentialsConfigured) { }

    @GetMapping public List<AccountResponse> list(@AuthenticationPrincipal AppPrincipal principal) {
        return accounts.findByUserIdOrderById(principal.id()).stream().map(this::response).toList();
    }

    @PostMapping public AccountResponse create(@AuthenticationPrincipal AppPrincipal principal,
                                                @Valid @RequestBody AccountRequest request) {
        if (request.gmailAppPassword() == null || request.gmailAppPassword().isBlank())
            throw new IllegalArgumentException("Gmail app password is required when creating an account");
        EmailReconciliationAccount value = new EmailReconciliationAccount(); value.setUserId(principal.id());
        apply(value, request, true); return response(accounts.save(value));
    }

    @PutMapping("/{id}") public AccountResponse update(@AuthenticationPrincipal AppPrincipal principal,
            @PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        EmailReconciliationAccount value = accounts.findByIdAndUserId(id, principal.id()).orElseThrow();
        apply(value, request, false); return response(accounts.save(value));
    }

    @DeleteMapping("/{id}") public void delete(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
        accounts.delete(accounts.findByIdAndUserId(id, principal.id()).orElseThrow());
    }

    private void apply(EmailReconciliationAccount value, AccountRequest request, boolean creating) {
        String lenderId=request.lenderId().trim().toUpperCase(), last4=request.bankAccountLast4().trim();
        String sender=request.bankSender().trim().toLowerCase(); long currentId=creating ? -1L : value.getId();
        if (!properties.isBankSender(sender)) throw new IllegalArgumentException(
                "Unsupported bank sender. Supported senders: " + properties.getBankSenders());
        if (accounts.existsByUserIdAndLenderIdIgnoreCaseAndIdNot(value.getUserId(), lenderId, currentId))
            throw new IllegalArgumentException("LenDenClub lender ID is already configured");
        if (accounts.existsByUserIdAndBankAccountLast4AndIdNot(value.getUserId(), last4, currentId))
            throw new IllegalArgumentException("Bank account is already configured");
        value.setLabel(request.label().trim()); value.setLenderId(lenderId);
        value.setMailboxEmail(request.mailboxEmail().trim().toLowerCase()); value.setBankSender(sender);
        value.setBankAccountLast4(last4); value.setEnabled(request.enabled());
        if (request.gmailAppPassword()!=null && !request.gmailAppPassword().isBlank())
            value.setEncryptedAppPassword(cipher.encrypt(request.gmailAppPassword().replace(" ", "")));
    }

    private AccountResponse response(EmailReconciliationAccount value) {
        return new AccountResponse(value.getId(),value.getLabel(),value.getLenderId(),value.getMailboxEmail(),
                value.getBankSender(),value.getBankAccountLast4(),value.isEnabled(),
                value.getEncryptedAppPassword()!=null && !value.getEncryptedAppPassword().isBlank());
    }
}
