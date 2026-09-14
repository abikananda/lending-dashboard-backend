package com.techconsulting.lending.service;

import com.techconsulting.lending.config.EmailReconciliationProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RepaymentEmailParserTest {
    private final EmailReconciliationProperties properties = properties();
    private final RepaymentEmailParser parser = new RepaymentEmailParser(properties);

    @Test
    void parsesAndValidatesLendenclubManualRepayment() {
        String body = """
                Hi SARASWATI PRUSTY,
                We've processed your repayment of ₹0.81. It will be credited to your bank account
                ending with XXXXXXXXXXXX6643 within 5 working days. Here's the breakdown:
                Processing Date Lending Type Principal Interest Total Amount
                Sept. 13, 2026 MANUAL LENDING ₹0.81 ₹0.00 ₹0.81
                Total ₹0.81 ₹0.00 ₹0.81
                """;
        var result = parser.parse(new EmailMessageData("m1", "noreply@lendenclub.com",
                "Repayment of ₹0.81 has been processed", Instant.now(), body)).orElseThrow();

        assertThat(result.type()).isEqualTo("LENDENCLUB_REPAYMENT");
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 9, 13));
        assertThat(result.principal()).isEqualByComparingTo("0.81");
        assertThat(result.interest()).isEqualByComparingTo("0.00");
        assertThat(result.total()).isEqualByComparingTo("0.81");
        assertThat(result.accountLast4()).isEqualTo("6643");
        assertThat(result.validationStatus()).isEqualTo("VALID");
    }

    @Test
    void parsesJanaBankCreditAlert() {
        String body = """
                Dear Customer,
                Your Jana Bank A/c no. XX6643 is credited with INR 48.08 on 12-SEP-2026.
                Info: IMPS 625516639634 INNOFIN
                Your account balance is INR 97,720.68.
                """;
        var result = parser.parse(new EmailMessageData("m2", "noreply@jana.bank.in",
                "Transaction Alert for your Jana Bank Account", Instant.now(), body)).orElseThrow();

        assertThat(result.type()).isEqualTo("BANK_CREDIT");
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(result.total()).isEqualByComparingTo("48.08");
        assertThat(result.accountLast4()).isEqualTo("6643");
        assertThat(result.reference()).isEqualTo("IMPS 625516639634 INNOFIN");
    }

    @Test
    void parsesSliceBankCreditAlert() {
        String body = """
                Hi ABIKANANDA,
                You have received ₹1,612.41 via IMPS in your slice bank a/c xx3003!
                Transaction Date 11-Sep-26
                """;
        var result = parser.parse(new EmailMessageData("m4", "noreply@slice.bank.in",
                "Received ₹1,612.41 via IMPS", Instant.now(), body)).orElseThrow();

        assertThat(result.type()).isEqualTo("BANK_CREDIT");
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 9, 11));
        assertThat(result.total()).isEqualByComparingTo("1612.41");
        assertThat(result.accountLast4()).isEqualTo("3003");
        assertThat(result.reference()).isNull();
    }

    @Test
    void flagsIncorrectPrincipalAndInterestBreakdown() {
        String body = """
                ending with XXXXXXXXXXXX6643 within 5 working days
                Sept. 13, 2026 MANUAL LENDING ₹0.70 ₹0.20 ₹0.81
                """;
        var result = parser.parse(new EmailMessageData("m3", "noreply@lendenclub.com",
                "Repayment of ₹0.81 has been processed", Instant.now(), body)).orElseThrow();

        assertThat(result.validationStatus()).isEqualTo("INVALID");
        assertThat(result.validationError()).contains("does not equal total");
    }

    private EmailReconciliationProperties properties() {
        EmailReconciliationProperties value = new EmailReconciliationProperties();
        value.setLendenclubSender("noreply@lendenclub.com");
        value.setBankSenders(List.of("noreply@jana.bank.in", "noreply@slice.bank.in"));
        return value;
    }
}
