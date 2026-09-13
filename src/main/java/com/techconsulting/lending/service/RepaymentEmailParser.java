package com.techconsulting.lending.service;

import com.techconsulting.lending.config.EmailReconciliationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RepaymentEmailParser {
    private static final String AMOUNT = "([0-9][0-9,]*(?:\\.[0-9]{1,2})?)";
    private static final Pattern LDC_AMOUNTS = Pattern.compile(
            "(?is)MANUAL\\s+LENDING.*?₹\\s*" + AMOUNT + ".*?₹\\s*" + AMOUNT + ".*?₹\\s*" + AMOUNT);
    private static final Pattern SUBJECT_AMOUNT = Pattern.compile("(?i)Repayment\\s+of\\s+₹\\s*" + AMOUNT);
    private static final Pattern LDC_DATE = Pattern.compile(
            "(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)\\.?\\s+(\\d{1,2}),\\s+(\\d{4})");
    private static final Pattern ACCOUNT = Pattern.compile("(?i)(?:ending\\s+with|A/c\\s+no\\.)\\s*X*(\\d{4})");
    private static final Pattern BANK = Pattern.compile("(?is)A/c\\s+no\\.\\s*X*(\\d{4})\\s+is\\s+credited\\s+with\\s+INR\\s+"
            + AMOUNT + "\\s+on\\s+(\\d{1,2}-[A-Z]{3}-\\d{4})\\.\\s*Info:\\s*([^\\r\\n]+)");
    private static final DateTimeFormatter BANK_DATE = new DateTimeFormatterBuilder().parseCaseInsensitive()
            .appendPattern("dd-MMM-uuuu").toFormatter(Locale.ENGLISH);
    private final EmailReconciliationProperties properties;

    public RepaymentEmailParser(EmailReconciliationProperties properties) {
        this.properties = properties;
    }

    public Optional<ParsedEmail> parse(EmailMessageData email) {
        String sender = email.sender() == null ? "" : email.sender().trim();
        if (sender.equalsIgnoreCase(properties.getLendenclubSender())) return Optional.of(parseLendenclub(email));
        if (sender.equalsIgnoreCase(properties.getBankSender())) return Optional.of(parseBank(email));
        return Optional.empty();
    }

    private ParsedEmail parseLendenclub(EmailMessageData email) {
        Matcher amounts = LDC_AMOUNTS.matcher(email.body());
        if (!amounts.find()) throw new IllegalArgumentException("LenDenClub principal, interest and total were not found");
        BigDecimal principal = amount(amounts.group(1));
        BigDecimal interest = amount(amounts.group(2));
        BigDecimal total = amount(amounts.group(3));
        Matcher date = LDC_DATE.matcher(email.body());
        if (!date.find()) throw new IllegalArgumentException("LenDenClub processing date was not found");
        String month = "Sept".equalsIgnoreCase(date.group(1)) ? "Sep" : date.group(1);
        LocalDate processingDate = LocalDate.parse(month + " " + date.group(2) + ", " + date.group(3),
                DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH));
        Matcher subjectAmount = SUBJECT_AMOUNT.matcher(email.subject());
        BigDecimal announced = subjectAmount.find() ? amount(subjectAmount.group(1)) : total;
        boolean valid = principal.add(interest).compareTo(total) == 0 && announced.compareTo(total) == 0;
        return new ParsedEmail("LENDENCLUB_REPAYMENT", processingDate, total, principal, interest,
                "MANUAL_LENDING", group(ACCOUNT, email.body(), 1), null,
                valid ? "VALID" : "INVALID", valid ? null : "Principal + interest or subject amount does not equal total");
    }

    private ParsedEmail parseBank(EmailMessageData email) {
        Matcher bank = BANK.matcher(email.body());
        if (!bank.find()) throw new IllegalArgumentException("Bank credit amount, date or reference was not found");
        return new ParsedEmail("BANK_CREDIT", LocalDate.parse(bank.group(3), BANK_DATE), amount(bank.group(2)),
                null, null, null, bank.group(1), bank.group(4).trim(), "VALID", null);
    }

    private BigDecimal amount(String value) { return new BigDecimal(value.replace(",", "")).setScale(2); }
    private String group(Pattern pattern, String value, int group) {
        Matcher matcher = pattern.matcher(value); return matcher.find() ? matcher.group(group) : null;
    }

    public record ParsedEmail(String type, LocalDate date, BigDecimal total, BigDecimal principal,
                              BigDecimal interest, String lendingType, String accountLast4,
                              String reference, String validationStatus, String validationError) { }
}
