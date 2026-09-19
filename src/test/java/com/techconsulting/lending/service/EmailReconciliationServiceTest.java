package com.techconsulting.lending.service;

import com.techconsulting.lending.config.EmailReconciliationProperties;
import com.techconsulting.lending.domain.EmailReconciliationAccount;
import com.techconsulting.lending.domain.PaymentNotification;
import com.techconsulting.lending.domain.User;
import com.techconsulting.lending.repository.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailReconciliationServiceTest {
    @Test
    void calculatesFiveWorkingDayCreditDeadline() {
        assertThat(EmailReconciliationService.addBusinessDays(LocalDate.of(2026, 9, 11), 5))
                .isEqualTo(LocalDate.of(2026, 9, 18));
    }

    @Test
    void relinksPreviouslyImportedEmailToConfiguredAccountBeforeReconciliation() {
        EmailReconciliationProperties properties = new EmailReconciliationProperties();
        properties.setLendenclubSender("noreply@lendenclub.com");
        properties.setBankSenders(List.of("noreply@slice.bank.in"));
        GmailImapClient gmail = mock(GmailImapClient.class);
        PaymentNotificationRepository notifications = mock(PaymentNotificationRepository.class);
        ReconciliationRecordRepository reconciliations = mock(ReconciliationRecordRepository.class);
        UserRepository users = mock(UserRepository.class);
        EmailReconciliationAccountRepository accounts = mock(EmailReconciliationAccountRepository.class);
        EmailCredentialCipher cipher = mock(EmailCredentialCipher.class);
        LumpsumRepaymentRepository lumpsumRepayments = mock(LumpsumRepaymentRepository.class);
        EmailReconciliationService service = new EmailReconciliationService(gmail,
                new RepaymentEmailParser(properties), notifications, mock(BankCreditRepository.class),
                reconciliations, lumpsumRepayments, users, accounts, cipher, properties);

        User user = new User(); user.setId(9L);
        EmailReconciliationAccount account = new EmailReconciliationAccount();
        account.setId(3L); account.setUserId(9L); account.setEnabled(true);
        account.setMailboxEmail("abikananda.2012@gmail.com");
        account.setEncryptedAppPassword("encrypted");
        account.setBankSender("noreply@slice.bank.in"); account.setBankAccountLast4("3003");
        PaymentNotification existing = new PaymentNotification();
        existing.setId(15L); existing.setUserId(9L); existing.setParsingStatus("PARSED");
        existing.setEmailMessageId("slice-message");
        EmailMessageData email = new EmailMessageData("slice-message", "noreply@slice.bank.in",
                "Received ₹3,745.55 via IMPS", Instant.now(), """
                You have received ₹3,745.55 via IMPS in your slice bank a/c xx3003!
                Transaction Date 19-Sep-26
                """);

        when(users.findById(9L)).thenReturn(Optional.of(user));
        when(accounts.findByUserIdOrderById(9L)).thenReturn(List.of(account));
        when(cipher.decrypt("encrypted")).thenReturn("app-password");
        when(gmail.fetch("abikananda.2012@gmail.com", "app-password", "noreply@slice.bank.in"))
                .thenReturn(List.of(email));
        when(notifications.findByUserIdAndEmailMessageId(9L, "slice-message"))
                .thenReturn(Optional.of(existing));
        when(notifications.save(any(PaymentNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notifications.findByUserIdAndNotificationTypeAndNotificationDateBetweenOrderByNotificationDateDesc(
                eq(9L), anyString(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
        when(reconciliations.findByUserIdAndReconciliationDateBetweenOrderByReconciliationDateDesc(
                eq(9L), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        var result = service.sync(9L);

        assertThat(result.duplicateEmails()).isEqualTo(1);
        assertThat(existing.getReconciliationAccountId()).isEqualTo(3L);
        verify(notifications).save(existing);
    }
}
