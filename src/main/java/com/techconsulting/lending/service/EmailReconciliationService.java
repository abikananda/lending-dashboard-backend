package com.techconsulting.lending.service;

import com.techconsulting.lending.config.EmailReconciliationProperties;
import com.techconsulting.lending.domain.*;
import com.techconsulting.lending.repository.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service
public class EmailReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(EmailReconciliationService.class);
    private final GmailImapClient gmail;
    private final RepaymentEmailParser parser;
    private final PaymentNotificationRepository notifications;
    private final BankCreditRepository bankCredits;
    private final ReconciliationRecordRepository reconciliations;
    private final UserRepository users;
    private final EmailReconciliationAccountRepository accounts;
    private final EmailCredentialCipher credentialCipher;
    private final EmailReconciliationProperties properties;
    private final Clock clock = Clock.system(ZoneId.of("Asia/Kolkata"));

    public EmailReconciliationService(GmailImapClient gmail, RepaymentEmailParser parser,
                                      PaymentNotificationRepository notifications,
                                      BankCreditRepository bankCredits,
                                      ReconciliationRecordRepository reconciliations,
                                      UserRepository users, EmailReconciliationAccountRepository accounts,
                                      EmailCredentialCipher credentialCipher,
                                      EmailReconciliationProperties properties) {
        this.gmail = gmail; this.parser = parser; this.notifications = notifications;
        this.bankCredits = bankCredits; this.reconciliations = reconciliations;
        this.users = users; this.accounts = accounts; this.credentialCipher = credentialCipher;
        this.properties = properties;
    }

    public SyncResult syncForConfiguredMailbox() {
        User user = users.findByEmailIgnoreCase(properties.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "No dashboard user exists for Gmail account " + properties.getUsername()));
        return sync(user.getId());
    }

    public synchronized SyncResult syncAllEnabled() {
        List<EmailReconciliationAccount> enabled = accounts.findByEnabledTrueOrderById();
        SyncResult total = new SyncResult(0, 0, 0, 0);
        Set<Long> usersWithDedicatedAccounts = new HashSet<>();
        boolean userMailboxAttempted = false;
        for (EmailReconciliationAccount account : enabled) {
            usersWithDedicatedAccounts.add(account.getUserId());
            try { total = total.add(syncAccount(account)); }
            catch (RuntimeException ex) { log.error("Email reconciliation failed for account id={} label={}",
                    account.getId(), account.getLabel(), ex); }
        }
        for (User user : users.findByEnabledTrueAndEmailPasswordIsNotNullOrderById()) {
            if (usersWithDedicatedAccounts.contains(user.getId())) continue;
            userMailboxAttempted = true;
            try { total = total.add(syncUserMailbox(user)); }
            catch (RuntimeException ex) { log.error("Email reconciliation failed for user id={}", user.getId(), ex); }
        }
        if (enabled.isEmpty() && !userMailboxAttempted
                && properties.getUsername() != null && !properties.getUsername().isBlank())
            return syncForConfiguredMailbox();
        return total;
    }

    public synchronized SyncResult sync(Long userId) {
        users.findById(userId).orElseThrow();
        List<EmailReconciliationAccount> configured = accounts.findByUserIdOrderById(userId).stream()
                .filter(EmailReconciliationAccount::isEnabled).toList();
        if (!configured.isEmpty()) {
            SyncResult total = new SyncResult(0, 0, 0, 0);
            for (EmailReconciliationAccount account : configured) total = total.add(syncAccount(account));
            return total;
        }
        User user = users.findById(userId).orElseThrow();
        if (user.getEmailPassword() != null && !user.getEmailPassword().isBlank()) return syncUserMailbox(user);
        if (user.getEmail().equalsIgnoreCase(properties.getUsername())) return importEmails(userId, null, gmail.fetch());
        throw new IllegalArgumentException("No email credentials or enabled reconciliation account is configured");
    }

    private SyncResult syncUserMailbox(User user) {
        return importEmails(user.getId(), null, gmail.fetch(user.getEmail(),
                credentialCipher.decrypt(user.getEmailPassword()), properties.getBankSenders()));
    }

    private SyncResult syncAccount(EmailReconciliationAccount account) {
        return importEmails(account.getUserId(), account,
                gmail.fetch(account.getMailboxEmail(), credentialCipher.decrypt(account.getEncryptedAppPassword()),
                        account.getBankSender()));
    }

    private SyncResult importEmails(Long userId, EmailReconciliationAccount account, List<EmailMessageData> emails) {
        int imported = 0, duplicates = 0, parseFailures = 0;
        for (EmailMessageData email : emails) {
            Optional<PaymentNotification> existing = notifications.findByUserIdAndEmailMessageId(
                    userId, truncate(email.messageId(), 255));
            if (existing.filter(value -> "PARSED".equals(value.getParsingStatus())).isPresent()) {
                duplicates++;
                continue;
            }
            try {
                Optional<RepaymentEmailParser.ParsedEmail> parsed = parser.parse(email);
                if (parsed.isEmpty()) continue;
                if (account != null && parsed.get().accountLast4() != null
                        && !account.getBankAccountLast4().equals(parsed.get().accountLast4()))
                    throw new IllegalArgumentException("Email account ending does not match configured bank account");
                PaymentNotification notification = saveNotification(userId, email, parsed.get(),
                        existing.orElseGet(PaymentNotification::new));
                notification.setReconciliationAccountId(account == null ? null : account.getId());
                notification = notifications.save(notification);
                if ("BANK_CREDIT".equals(parsed.get().type())) saveBankCredit(userId, notification, parsed.get());
                imported++;
            } catch (RuntimeException ex) {
                PaymentNotification failed = existing.orElseGet(PaymentNotification::new);
                failed.setReconciliationAccountId(account == null ? null : account.getId());
                saveParseFailure(userId, email, ex, failed);
                parseFailures++;
            }
        }
        int reconciled = reconcile(userId, account == null ? null : account.getId());
        return new SyncResult(imported, duplicates, parseFailures, reconciled);
    }

    public List<ReconciliationRecord> records(Long userId, LocalDate from, LocalDate to) {
        return reconciliations.findByUserIdAndReconciliationDateBetweenOrderByReconciliationDateDesc(userId, from, to);
    }

    private PaymentNotification saveNotification(Long userId, EmailMessageData email,
                                                 RepaymentEmailParser.ParsedEmail parsed,
                                                 PaymentNotification value) {
        populateBase(value, userId, email);
        value.setNotificationType(parsed.type()); value.setNotificationDate(parsed.date());
        value.setReportedAmount(parsed.total()); value.setPrincipalAmount(parsed.principal());
        value.setInterestAmount(parsed.interest()); value.setLendingType(parsed.lendingType());
        value.setAccountLast4(parsed.accountLast4()); value.setTransactionReference(truncate(parsed.reference(), 160));
        value.setAmountValidationStatus(parsed.validationStatus()); value.setParsingStatus("PARSED");
        value.setParsingError(parsed.validationError());
        return notifications.save(value);
    }

    private void saveParseFailure(Long userId, EmailMessageData email, RuntimeException error,
                                  PaymentNotification value) {
        populateBase(value, userId, email);
        value.setNotificationType(properties.isBankSender(email.sender())
                ? "BANK_CREDIT" : "LENDENCLUB_REPAYMENT");
        value.setParsingStatus("FAILED"); value.setAmountValidationStatus("NOT_VALIDATED");
        value.setParsingError(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        notifications.save(value);
    }

    private void populateBase(PaymentNotification value, Long userId, EmailMessageData email) {
        value.setUserId(userId); value.setEmailMessageId(truncate(email.messageId(), 255));
        value.setEmailReceivedAt(email.receivedAt()); value.setSender(truncate(email.sender(), 255));
        value.setSubject(truncate(email.subject(), 500)); value.setRawEmailText(email.body());
    }

    private BankCredit saveBankCredit(Long userId, PaymentNotification notification,
                                      RepaymentEmailParser.ParsedEmail parsed) {
        if (parsed.reference() != null) {
            Optional<BankCredit> existing = bankCredits
                    .findFirstByUserIdAndBankTransactionReferenceIgnoreCase(userId, parsed.reference());
            if (existing.isPresent()) return existing.get();
        }
        String fingerprint = sha256(userId + "|EMAIL|" + notification.getEmailMessageId());
        return bankCredits.findByUserIdAndRowFingerprint(userId, fingerprint).orElseGet(() -> {
            BankCredit credit = new BankCredit(); credit.setUserId(userId);
            credit.setBankTransactionReference(truncate(parsed.reference(), 160)); credit.setRowFingerprint(fingerprint);
            credit.setTransactionDate(parsed.date()); credit.setAmount(parsed.total());
            credit.setDescription(truncate(notification.getRawEmailText(), 1000));
            credit.setSource(notification.getSender().equalsIgnoreCase("noreply@slice.bank.in")
                    ? "EMAIL_SLICE_BANK" : "EMAIL_JANA_BANK");
            return bankCredits.save(credit);
        });
    }

    private int reconcile(Long userId, Long accountId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(Math.max(1, properties.getLookbackDays()));
        List<PaymentNotification> lender = notifications
                .findByUserIdAndNotificationTypeAndNotificationDateBetweenOrderByNotificationDateDesc(
                        userId, "LENDENCLUB_REPAYMENT", from, today);
        List<PaymentNotification> bank = notifications
                .findByUserIdAndNotificationTypeAndNotificationDateBetweenOrderByNotificationDateDesc(
                        userId, "BANK_CREDIT", from, today.plusDays(7));
        if (accountId != null) {
            lender = lender.stream().filter(value -> accountId.equals(value.getReconciliationAccountId())).toList();
            bank = bank.stream().filter(value -> accountId.equals(value.getReconciliationAccountId())).toList();
        }
        Set<Long> usedBankNotifications = new HashSet<>();
        reconciliations.findByUserIdAndReconciliationDateBetweenOrderByReconciliationDateDesc(userId, from, today)
                .stream().map(ReconciliationRecord::getBankPaymentNotificationId).filter(Objects::nonNull)
                .forEach(usedBankNotifications::add);
        int updated = 0;
        for (PaymentNotification repayment : lender) {
            if (!"PARSED".equals(repayment.getParsingStatus())) continue;
            ReconciliationRecord record = reconciliations
                    .findByUserIdAndPaymentNotificationId(userId, repayment.getId())
                    .orElseGet(ReconciliationRecord::new);
            if (record.isManuallyResolved()) continue;
            record.setUserId(userId); record.setPaymentNotificationId(repayment.getId());
            record.setReconciliationDate(repayment.getNotificationDate());
            record.setLenderReportedAmount(repayment.getReportedAmount());
            usedBankNotifications.remove(record.getBankPaymentNotificationId());
            LocalDate due = addBusinessDays(repayment.getNotificationDate(), 5);
            Optional<PaymentNotification> match = bank.stream().filter(value -> "PARSED".equals(value.getParsingStatus()))
                    .filter(value -> !usedBankNotifications.contains(value.getId()))
                    .filter(value -> !value.getNotificationDate().isBefore(repayment.getNotificationDate()))
                    .filter(value -> !value.getNotificationDate().isAfter(due))
                    .filter(value -> accountMatches(repayment, value))
                    .filter(value -> value.getReportedAmount().compareTo(repayment.getReportedAmount()) == 0)
                    .findFirst();
            applyStatus(record, repayment, match, bank, due, today, userId);
            match.ifPresent(value -> usedBankNotifications.add(value.getId()));
            reconciliations.save(record); updated++;
        }
        return updated;
    }

    private void applyStatus(ReconciliationRecord record, PaymentNotification repayment,
                             Optional<PaymentNotification> match, List<PaymentNotification> bank,
                             LocalDate due, LocalDate today, Long userId) {
        if (!"VALID".equals(repayment.getAmountValidationStatus())) {
            record.setBankPaymentNotificationId(null); record.setBankCreditId(null);
            record.setStatus("INVALID_EMI_BREAKDOWN"); record.setReason(repayment.getParsingError());
            record.setDifferenceAmount(BigDecimal.ZERO); return;
        }
        if (match.isPresent()) {
            PaymentNotification bankMail = match.get();
            record.setBankPaymentNotificationId(bankMail.getId());
            record.setStatus("MATCHED"); record.setReason("LenDenClub repayment matched bank credit");
            record.setBankCreditedAmount(bankMail.getReportedAmount());
            record.setDifferenceAmount(bankMail.getReportedAmount().subtract(repayment.getReportedAmount()));
            if (bankMail.getTransactionReference() != null) bankCredits
                    .findFirstByUserIdAndBankTransactionReferenceIgnoreCase(userId, bankMail.getTransactionReference())
                    .ifPresent(value -> record.setBankCreditId(value.getId()));
            return;
        }
        record.setBankCreditedAmount(BigDecimal.ZERO);
        record.setBankPaymentNotificationId(null);
        record.setBankCreditId(null);
        record.setDifferenceAmount(repayment.getReportedAmount().negate());
        if (!today.isAfter(due)) {
            record.setStatus("PENDING_BANK_CREDIT"); record.setReason("Bank credit expected by " + due);
        } else {
            boolean otherAmount = bank.stream().anyMatch(value -> value.getNotificationDate() != null
                    && !value.getNotificationDate().isBefore(repayment.getNotificationDate())
                    && !value.getNotificationDate().isAfter(due) && accountMatches(repayment, value));
            record.setStatus(otherAmount ? "AMOUNT_MISMATCH" : "BANK_CREDIT_MISSING");
            record.setReason(otherAmount ? "Bank credit found in the payout window but amount differs"
                    : "No matching bank credit within five working days");
        }
    }

    private boolean accountMatches(PaymentNotification left, PaymentNotification right) {
        return left.getAccountLast4() == null || right.getAccountLast4() == null
                || left.getAccountLast4().equals(right.getAccountLast4());
    }

    static LocalDate addBusinessDays(LocalDate date, int days) {
        LocalDate result = date; int added = 0;
        while (added < days) { result = result.plusDays(1); DayOfWeek day = result.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) added++; }
        return result;
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException("Unable to fingerprint bank email", ex); }
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record SyncResult(int importedEmails, int duplicateEmails, int parsingFailures,
                             int reconciliationsUpdated) {
        SyncResult add(SyncResult other) {
            return new SyncResult(importedEmails + other.importedEmails, duplicateEmails + other.duplicateEmails,
                    parsingFailures + other.parsingFailures, reconciliationsUpdated + other.reconciliationsUpdated);
        }
    }
}
