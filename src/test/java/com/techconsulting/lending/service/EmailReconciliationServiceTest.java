package com.techconsulting.lending.service;

import com.techconsulting.lending.config.EmailReconciliationProperties;
import com.techconsulting.lending.domain.EmailReconciliationAccount;
import com.techconsulting.lending.domain.LumpsumRepayment;
import com.techconsulting.lending.domain.PaymentNotification;
import com.techconsulting.lending.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailReconciliationServiceTest {
    @Test
    void calculatesFiveWorkingDayCreditDeadline() {
        assertThat(EmailReconciliationService.addBusinessDays(LocalDate.of(2026, 9, 11), 5))
                .isEqualTo(LocalDate.of(2026, 9, 18));
    }

    @Test
    void backfillsLumpsumFromStoredPaymentNotificationWithoutRefetchingEmail() {
        EmailReconciliationProperties properties = new EmailReconciliationProperties();
        properties.setLendenclubSender("noreply@lendenclub.com");
        properties.setBankSenders(List.of("noreply@jana.bank.in", "noreply@slice.bank.in"));
        LumpsumRepaymentRepository lumpsums = mock(LumpsumRepaymentRepository.class);
        when(lumpsums.findByPaymentNotificationId(21L)).thenReturn(Optional.empty());
        when(lumpsums.save(any(LumpsumRepayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailReconciliationService service = new EmailReconciliationService(mock(GmailImapClient.class),
                new RepaymentEmailParser(properties), mock(PaymentNotificationRepository.class),
                mock(BankCreditRepository.class), mock(ReconciliationRecordRepository.class), lumpsums,
                mock(UserRepository.class), mock(EmailReconciliationAccountRepository.class),
                mock(EmailCredentialCipher.class), properties);
        PaymentNotification notification = new PaymentNotification();
        notification.setId(21L);
        notification.setUserId(9L);
        notification.setReconciliationAccountId(3L);
        notification.setEmailMessageId("stored-lendenclub-email");
        notification.setEmailReceivedAt(Instant.parse("2026-09-19T09:30:00Z"));
        notification.setSender("noreply@lendenclub.com");
        notification.setSubject("Repayment of ₹5414.94 has been processed to your bank account XXXXXXXXXXXX6643");
        notification.setRawEmailText("""
                ending with XXXXXXXXXXXX6643 within 5 working days
                Sept. 19, 2026 LUMPSUM ₹240.71 ₹13.65 ₹254.36
                Sept. 19, 2026 MANUAL LENDING ₹4767.98 ₹392.60 ₹5160.58
                Total ₹5008.69 ₹406.25 ₹5414.94
                """);

        LumpsumRepayment result = service.findOrBackfillLumpsum(notification).orElseThrow();

        assertThat(result.getUserId()).isEqualTo(9L);
        assertThat(result.getReconciliationAccountId()).isEqualTo(3L);
        assertThat(result.getPaymentNotificationId()).isEqualTo(21L);
        assertThat(result.getProcessingDate()).isEqualTo(LocalDate.of(2026, 9, 19));
        assertThat(result.getPrincipalAmount()).isEqualByComparingTo("240.71");
        assertThat(result.getInterestAmount()).isEqualByComparingTo("13.65");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("254.36");
    }

    @Test
    void relinksStoredSecondUserNotificationAndBackfillsItsLumpsum() {
        EmailReconciliationProperties properties = new EmailReconciliationProperties();
        properties.setLendenclubSender("noreply@lendenclub.com");
        properties.setBankSenders(List.of("noreply@slice.bank.in"));
        PaymentNotificationRepository notifications = mock(PaymentNotificationRepository.class);
        LumpsumRepaymentRepository lumpsums = mock(LumpsumRepaymentRepository.class);
        when(lumpsums.findByPaymentNotificationId(31L)).thenReturn(Optional.empty());
        when(lumpsums.save(any(LumpsumRepayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EmailReconciliationService service = new EmailReconciliationService(mock(GmailImapClient.class),
                new RepaymentEmailParser(properties), notifications, mock(BankCreditRepository.class),
                mock(ReconciliationRecordRepository.class), lumpsums, mock(UserRepository.class),
                mock(EmailReconciliationAccountRepository.class), mock(EmailCredentialCipher.class), properties);
        EmailReconciliationAccount account = new EmailReconciliationAccount();
        account.setId(8L);
        account.setUserId(12L);
        account.setBankAccountLast4("3003");
        PaymentNotification notification = new PaymentNotification();
        notification.setId(31L);
        notification.setUserId(12L);
        notification.setAccountLast4("3003");
        notification.setEmailMessageId("second-user-lendenclub-email");
        notification.setEmailReceivedAt(Instant.parse("2026-09-19T09:30:00Z"));
        notification.setSender("noreply@lendenclub.com");
        notification.setSubject("Repayment of ₹5414.94 has been processed to your bank account XXXXXXXXXXXX3003");
        notification.setRawEmailText("""
                ending with XXXXXXXXXXXX3003 within 5 working days
                Sept. 19, 2026 LUMPSUM ₹240.71 ₹13.65 ₹254.36
                Sept. 19, 2026 MANUAL LENDING ₹4767.98 ₹392.60 ₹5160.58
                Total ₹5008.69 ₹406.25 ₹5414.94
                """);
        when(notifications.findStoredLendenclubEmailsMissingLumpsumRepayment(12L))
                .thenReturn(List.of(notification));

        service.backfillStoredLumpsums(12L, account);

        assertThat(notification.getReconciliationAccountId()).isEqualTo(8L);
        verify(notifications).save(notification);
        ArgumentCaptor<LumpsumRepayment> savedValue = ArgumentCaptor.forClass(LumpsumRepayment.class);
        verify(lumpsums).save(savedValue.capture());
        LumpsumRepayment saved = savedValue.getValue();
        assertThat(saved.getUserId()).isEqualTo(12L);
        assertThat(saved.getReconciliationAccountId()).isEqualTo(8L);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("254.36");
    }
}
