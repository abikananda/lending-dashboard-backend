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
                "Repayment of ₹0.81 has been processed to your bank account XXXXXXXXXXXX6643",
                Instant.now(), body)).orElseThrow();

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
                Your Jana Bank A/c no. XX6643 is credited with INR 4,508.60 on 16-SEP-2026.
                Info: IMPS 625918610771 INNOFIN
                Your account balance is INR 97,720.68.
                """;
        var result = parser.parse(new EmailMessageData("m2", "noreply@jana.bank.in",
                "Transaction Alert for your Jana Bank Account", Instant.now(), body)).orElseThrow();

        assertThat(result.type()).isEqualTo("BANK_CREDIT");
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 9, 16));
        assertThat(result.total()).isEqualByComparingTo("4508.60");
        assertThat(result.accountLast4()).isEqualTo("6643");
        assertThat(result.reference()).isEqualTo("IMPS 625918610771 INNOFIN");
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
    void parsesSliceEmailContainingInvisibleUnicodeFormattingCharacters() {
        String body = """
                You have received ₹3,‌745.55 via IMPS in your slice bank a/c xx3003!
                Transaction Date 19-Sep-26
                Sender Name INNOFINSOLUTIONSPRIVATELIMITED
                IMPS Ref No. 626220618806
                """;

        var result = parser.parse(new EmailMessageData("m-unicode", "noreply@slice.bank.in",
                "Received ₹3,‌745.55 via IMPS", Instant.now(), body)).orElseThrow();

        assertThat(result.total()).isEqualByComparingTo("3745.55");
        assertThat(result.accountLast4()).isEqualTo("3003");
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 9, 19));
    }

    @Test
    void flagsIncorrectPrincipalAndInterestBreakdown() {
        String body = """
                ending with XXXXXXXXXXXX6643 within 5 working days
                Sept. 13, 2026 MANUAL LENDING ₹0.70 ₹0.20 ₹0.81
                Total ₹0.70 ₹0.20 ₹0.81
                """;
        var result = parser.parse(new EmailMessageData("m3", "noreply@lendenclub.com",
                "Repayment of ₹0.81 has been processed to your bank account XXXXXXXXXXXX6643",
                Instant.now(), body)).orElseThrow();

        assertThat(result.validationStatus()).isEqualTo("INVALID");
        assertThat(result.validationError()).contains("does not equal LenDenClub total");
    }

    @Test
    void parsesLumpsumAndKeepsManualRepaymentValuesSeparate() {
        String body = """
                We've processed your repayment of ₹5414.94. It will be credited to your bank account
                ending with XXXXXXXXXXXX6643 within 5 working days. Here's the breakdown:
                Processing Date Lending Type Principal Interest Total Amount
                Sept. 19, 2026 LUMPSUM ₹240.71 ₹13.65 ₹254.36
                Sept. 19, 2026 MANUAL LENDING ₹4767.98 ₹392.60 ₹5160.58
                Total ₹5008.69 ₹406.25 ₹5414.94
                """;

        var result = parser.parse(new EmailMessageData("m10", "noreply@lendenclub.com",
                "Repayment of ₹5414.94 has been processed to your bank account XXXXXXXXXXXX6643",
                Instant.now(), body)).orElseThrow();

        assertThat(result.total()).isEqualByComparingTo("5160.58");
        assertThat(result.principal()).isEqualByComparingTo("4767.98");
        assertThat(result.interest()).isEqualByComparingTo("392.60");
        assertThat(result.lumpsumPrincipal()).isEqualByComparingTo("240.71");
        assertThat(result.lumpsumInterest()).isEqualByComparingTo("13.65");
        assertThat(result.lumpsumTotal()).isEqualByComparingTo("254.36");
        assertThat(result.lenderReportedAmount()).isEqualByComparingTo("5414.94");
        assertThat(result.validationStatus()).isEqualTo("VALID");
    }

    @Test
    void flagsCombinedBodyTotalThatDoesNotMatchSubject() {
        String body = """
                ending with XXXXXXXXXXXX6643 within 5 working days
                Sept. 19, 2026 LUMPSUM ₹240.71 ₹13.65 ₹254.36
                Sept. 19, 2026 MANUAL LENDING ₹4767.98 ₹392.60 ₹5160.58
                Total ₹5008.69 ₹406.25 ₹5414.94
                """;

        var result = parser.parse(new EmailMessageData("m11", "noreply@lendenclub.com",
                "Repayment of ₹5415.94 has been processed to your bank account XXXXXXXXXXXX6643",
                Instant.now(), body)).orElseThrow();

        assertThat(result.validationStatus()).isEqualTo("INVALID");
    }

    @Test
    void ignoresJanaDebitAlerts() {
        String body = """
                Dear Customer,
                Your Jana Bank A/c no. XX6643 is debited with INR 15,000.00 on 16-SEP-2026.
                Info: UPI/DR/129733177918/ABIKA.
                Your account balance is INR 85,197.95.
                """;

        assertThat(parser.parse(new EmailMessageData("m12", "noreply@jana.bank.in",
                "Transaction Alert for your Jana Bank Account", Instant.now(), body))).isEmpty();
    }

    @Test
    void ignoresJanaCreditsThatAreNotFromInnOfin() {
        String body = """
                Your Jana Bank A/c no. XX6643 is credited with INR 2,000.00 on 16-SEP-2026.
                Info: IMPS 625918610772 OTHER PARTY
                """;

        assertThat(parser.parse(new EmailMessageData("m13", "noreply@jana.bank.in",
                "Transaction Alert for your Jana Bank Account", Instant.now(), body))).isEmpty();
    }

    @Test
    void ignoresOtherEmailsFromKnownSenders() {
        assertThat(parser.parse(new EmailMessageData("m5", "noreply@lendenclub.com",
                "Your monthly statement is ready", Instant.now(), "Repayment of ₹100.00"))).isEmpty();
        assertThat(parser.parse(new EmailMessageData("m6", "noreply@jana.bank.in",
                "Your account statement", Instant.now(), "credited with INR 100.00"))).isEmpty();
        assertThat(parser.parse(new EmailMessageData("m7", "noreply@slice.bank.in",
                "Payment reminder", Instant.now(), "received ₹100.00"))).isEmpty();
    }

    @Test
    void requiresTheCompleteExpectedSubjectFormat() {
        assertThat(parser.parse(new EmailMessageData("m8", "noreply@lendenclub.com",
                "Repayment of ₹0.81 has been processed", Instant.now(), "irrelevant"))).isEmpty();
        assertThat(parser.parse(new EmailMessageData("m9", "noreply@slice.bank.in",
                "Received ₹1,612.41", Instant.now(), "irrelevant"))).isEmpty();
    }

    private EmailReconciliationProperties properties() {
        EmailReconciliationProperties value = new EmailReconciliationProperties();
        value.setLendenclubSender("noreply@lendenclub.com");
        value.setBankSenders(List.of("noreply@jana.bank.in", "noreply@slice.bank.in"));
        return value;
    }
}
