package com.techconsulting.lending.service;

import com.techconsulting.lending.config.EmailReconciliationProperties;
import com.techconsulting.lending.domain.EmailReconciliationAccount;
import com.techconsulting.lending.domain.LumpsumRepayment;
import com.techconsulting.lending.domain.PaymentNotification;
import com.techconsulting.lending.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailReconciliationServiceTest {
    @Test
    void calculatesFiveWorkingDayCreditDeadline() {
        assertThat(EmailReconciliationService.addBusinessDays(LocalDate.of(2026, 9, 11), 5))
                .isEqualTo(LocalDate.of(2026, 9, 18));
    }

    @Test
    void routesOnlyEmailsWithTheConfiguredBankAccountEnding() {
        EmailReconciliationProperties properties = new EmailReconciliationProperties();
        EmailReconciliationService service = new EmailReconciliationService(mock(GmailImapClient.class),
                mock(RepaymentEmailParser.class), mock(PaymentNotificationRepository.class),
                mock(BankCreditRepository.class), mock(ReconciliationRecordRepository.class),
                mock(LumpsumRepaymentRepository.class), mock(UserRepository.class),
                mock(EmailReconciliationAccountRepository.class), mock(EmailCredentialCipher.class), properties);
        EmailReconciliationAccount account = new EmailReconciliationAccount();
        account.setBankAccountLast4("3003");

        assertThat(service.belongsToAccount(account, parsedForAccount("3003"))).isTrue();
        assertThat(service.belongsToAccount(account, parsedForAccount("6643"))).isFalse();
        assertThat(service.belongsToAccount(account, parsedForAccount(null))).isFalse();
        assertThat(service.belongsToAccount(null, parsedForAccount(null))).isTrue();
    }

    @Test
    void persistsLumpsumOnlyFromParsedLendenclubEmail() {
        EmailReconciliationProperties properties = new EmailReconciliationProperties();
        LumpsumRepaymentRepository lumpsums = mock(LumpsumRepaymentRepository.class);
        when(lumpsums.findByPaymentNotificationId(21L)).thenReturn(Optional.empty());
        when(lumpsums.saveAndFlush(any(LumpsumRepayment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmailReconciliationService service = new EmailReconciliationService(mock(GmailImapClient.class),
                mock(RepaymentEmailParser.class), mock(PaymentNotificationRepository.class),
                mock(BankCreditRepository.class), mock(ReconciliationRecordRepository.class), lumpsums,
                mock(UserRepository.class), mock(EmailReconciliationAccountRepository.class),
                mock(EmailCredentialCipher.class), properties);
        EmailReconciliationAccount account = new EmailReconciliationAccount();
        account.setId(3L);
        PaymentNotification notification = new PaymentNotification();
        notification.setId(21L);
        RepaymentEmailParser.ParsedEmail parsed = new RepaymentEmailParser.ParsedEmail(
                "LENDENCLUB_REPAYMENT", LocalDate.of(2026, 9, 19), new BigDecimal("5160.58"),
                new BigDecimal("4767.98"), new BigDecimal("392.60"), "MANUAL_LENDING", "6643",
                null, "VALID", null, new BigDecimal("5414.94"), new BigDecimal("240.71"),
                new BigDecimal("13.65"), new BigDecimal("254.36"));

        LumpsumRepayment result = service.persistLumpsumFromLendenclubParse(
                9L, account, notification, parsed).orElseThrow();

        assertThat(result.getUserId()).isEqualTo(9L);
        assertThat(result.getReconciliationAccountId()).isEqualTo(3L);
        assertThat(result.getPaymentNotificationId()).isEqualTo(21L);
        assertThat(result.getProcessingDate()).isEqualTo(LocalDate.of(2026, 9, 19));
        assertThat(result.getPrincipalAmount()).isEqualByComparingTo("240.71");
        assertThat(result.getInterestAmount()).isEqualByComparingTo("13.65");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("254.36");
    }

    @Test
    void doesNotCreateLumpsumForBankEmail() {
        EmailReconciliationProperties properties = new EmailReconciliationProperties();
        LumpsumRepaymentRepository lumpsums = mock(LumpsumRepaymentRepository.class);
        EmailReconciliationService service = new EmailReconciliationService(mock(GmailImapClient.class),
                mock(RepaymentEmailParser.class), mock(PaymentNotificationRepository.class),
                mock(BankCreditRepository.class),
                mock(ReconciliationRecordRepository.class), lumpsums, mock(UserRepository.class),
                mock(EmailReconciliationAccountRepository.class), mock(EmailCredentialCipher.class), properties);
        PaymentNotification notification = new PaymentNotification();
        notification.setId(31L);
        RepaymentEmailParser.ParsedEmail parsed = parsedForAccount("3003");

        assertThat(service.persistLumpsumFromLendenclubParse(12L, null, notification, parsed)).isEmpty();
    }

    private RepaymentEmailParser.ParsedEmail parsedForAccount(String accountLast4) {
        return new RepaymentEmailParser.ParsedEmail("BANK_CREDIT", LocalDate.of(2026, 9, 19),
                new BigDecimal("100.00"), null, null, null, accountLast4, null,
                "VALID", null, null, null, null, null);
    }
}
