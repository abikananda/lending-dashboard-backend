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
    private static final Pattern LENDENCLUB_SUBJECT = Pattern.compile("(?i)^\\s*Repayment\\s+of\\s+₹\\s*"
            + AMOUNT + "\\s+has\\s+been\\s+processed\\s+to\\s+your\\s+bank\\s+account\\s+X*\\d{4}\\s*$");
    private static final Pattern JANA_SUBJECT = Pattern.compile(
            "(?i)^\\s*Transaction\\s+Alert\\s+for\\s+your\\s+Jana\\s+Bank\\s+Account\\s*$");
    private static final Pattern SLICE_SUBJECT = Pattern.compile(
            "(?i)^\\s*Received\\s+₹\\s*" + AMOUNT + "\\s+via\\s+IMPS\\s*$");
    private static final Pattern LDC_DATE = Pattern.compile(
            "(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)\\.?\\s+(\\d{1,2}),\\s+(\\d{4})");
    private static final Pattern ACCOUNT = Pattern.compile("(?i)(?:ending\\s+with|A/c\\s+no\\.)\\s*X*(\\d{4})");
    private static final Pattern JANA_BANK = Pattern.compile("(?is)A/c\\s+no\\.\\s*X*(\\d{4})\\s+is\\s+credited\\s+with\\s+INR\\s+"
            + AMOUNT + "\\s+on\\s+(\\d{1,2}-[A-Z]{3}-\\d{4})\\.\\s*Info:\\s*([^\\r\\n]+)");
    private static final Pattern SLICE_BANK = Pattern.compile("(?is)(?:received\\s+)?₹\\s*" + AMOUNT
            + "\\s+via\\s+IMPS\\s+in\\s+your\\s+slice\\s+bank\\s+a/c\\s+x*(\\d{4}).*?"
            + "Transaction\\s+Date\\s+(\\d{1,2}-[A-Z]{3}-\\d{2,4})");
    private static final DateTimeFormatter BANK_DATE = new DateTimeFormatterBuilder().parseCaseInsensitive()
            .appendPattern("dd-MMM-uuuu").toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter SLICE_DATE = new DateTimeFormatterBuilder().parseCaseInsensitive()
            .appendPattern("d-MMM-").appendValueReduced(java.time.temporal.ChronoField.YEAR, 2, 2, 2000)
            .toFormatter(Locale.ENGLISH);
    private final EmailReconciliationProperties properties;

    public RepaymentEmailParser(EmailReconciliationProperties properties) {
        this.properties = properties;
    }

    public Optional<ParsedEmail> parse(EmailMessageData email) {
        String sender = email.sender() == null ? "" : email.sender().trim();
        if (!matchesExpectedSubject(sender, email.subject(), properties)) return Optional.empty();
        if (sender.equalsIgnoreCase(properties.getLendenclubSender())) return Optional.of(parseLendenclub(email));
        if (properties.isBankSender(sender)) return Optional.of(parseBank(email));
        return Optional.empty();
    }

    static boolean matchesExpectedSubject(String sender, String subject,
                                          EmailReconciliationProperties properties) {
        if (sender == null || subject == null) return false;
        if (sender.trim().equalsIgnoreCase(properties.getLendenclubSender()))
            return LENDENCLUB_SUBJECT.matcher(subject).matches();
        if (sender.trim().equalsIgnoreCase("noreply@jana.bank.in") && properties.isBankSender(sender.trim()))
            return JANA_SUBJECT.matcher(subject).matches();
        if (sender.trim().equalsIgnoreCase("noreply@slice.bank.in") && properties.isBankSender(sender.trim()))
            return SLICE_SUBJECT.matcher(subject).matches();
        return false;
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
        if (email.sender().equalsIgnoreCase("noreply@slice.bank.in")) {
            Matcher slice = SLICE_BANK.matcher(email.body());
            if (!slice.find()) throw new IllegalArgumentException("Slice credit amount, account or date was not found");
            return new ParsedEmail("BANK_CREDIT", LocalDate.parse(slice.group(3), SLICE_DATE), amount(slice.group(1)),
                    null, null, null, slice.group(2), null, "VALID", null);
        }
        Matcher bank = JANA_BANK.matcher(email.body());
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
