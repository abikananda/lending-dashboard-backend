package com.techconsulting.lending.service;

import com.techconsulting.lending.config.EmailReconciliationProperties;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.FromStringTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class GmailImapClient {
    private final EmailReconciliationProperties properties;

    public GmailImapClient(EmailReconciliationProperties properties) {
        this.properties = properties;
    }

    public List<EmailMessageData> fetch() {
        validateConfiguration();
        return fetch(properties.getUsername(), properties.getAppPassword(), properties.getBankSenders());
    }

    public List<EmailMessageData> fetch(String username, String appPassword, String bankSender) {
        return fetch(username, appPassword, List.of(bankSender));
    }

    public List<EmailMessageData> fetch(String username, String appPassword, List<String> bankSenders) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Mailbox email is required");
        if (appPassword == null || appPassword.isBlank()) throw new IllegalArgumentException("Gmail app password is required");
        if (bankSenders == null || bankSenders.isEmpty()) throw new IllegalArgumentException("At least one bank sender is required");
        return fetchMailbox(username, appPassword, bankSenders);
    }

    private List<EmailMessageData> fetchMailbox(String username, String appPassword, List<String> bankSenders) {
        Properties mail = new Properties();
        mail.put("mail.store.protocol", "imaps");
        mail.put("mail.imaps.host", properties.getHost());
        mail.put("mail.imaps.port", Integer.toString(properties.getPort()));
        mail.put("mail.imaps.ssl.enable", "true");
        mail.put("mail.imaps.connectiontimeout", "10000");
        mail.put("mail.imaps.timeout", "15000");

        List<EmailMessageData> result = new ArrayList<>();
        try (Store store = Session.getInstance(mail).getStore("imaps")) {
            store.connect(properties.getHost(), properties.getPort(), username, appPassword);
            Folder folder = store.getFolder(properties.getFolder());
            try {
                folder.open(Folder.READ_ONLY);
                Date since = Date.from(Instant.now().minus(Math.max(1, properties.getLookbackDays()), ChronoUnit.DAYS));
                List<SearchTerm> expectedMessages = new ArrayList<>();
                expectedMessages.add(senderAndSubject(properties.getLendenclubSender(), "Repayment of"));
                bankSenders.stream().map(this::bankSenderAndSubject)
                        .filter(Objects::nonNull).forEach(expectedMessages::add);
                var senderAndSubject = new OrTerm(expectedMessages.toArray(SearchTerm[]::new));
                Message[] messages = folder.search(new AndTerm(
                        new ReceivedDateTerm(ComparisonTerm.GE, since), senderAndSubject));
                for (Message message : messages) {
                    String sender = address(message.getFrom());
                    String subject = Objects.toString(message.getSubject(), "");
                    if (!RepaymentEmailParser.matchesExpectedSubject(sender, subject, properties)) continue;
                    String body = body(message);
                    Date received = Optional.ofNullable(message.getReceivedDate())
                            .orElse(Optional.ofNullable(message.getSentDate()).orElse(new Date()));
                    result.add(new EmailMessageData(messageId(message, body), sender,
                            subject, received.toInstant(), body));
                }
            } finally {
                if (folder.isOpen()) folder.close(false);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read repayment emails from Gmail", ex);
        }
        return result;
    }

    private SearchTerm bankSenderAndSubject(String sender) {
        if (sender == null) return null;
        if (sender.equalsIgnoreCase("noreply@jana.bank.in"))
            return senderAndSubject(sender, "Transaction Alert for your Jana Bank Account");
        if (sender.equalsIgnoreCase("noreply@slice.bank.in"))
            return senderAndSubject(sender, "Received");
        return null;
    }

    private SearchTerm senderAndSubject(String sender, String subject) {
        return new AndTerm(new FromStringTerm(sender), new SubjectTerm(subject));
    }

    private String body(Part part) throws Exception {
        if (part.isMimeType("text/plain")) return Objects.toString(part.getContent(), "");
        if (part.isMimeType("text/html")) return Jsoup.parse(Objects.toString(part.getContent(), "")).text();
        if (part.getContent() instanceof Multipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) text.append(' ').append(body(multipart.getBodyPart(i)));
            return text.toString().trim();
        }
        return "";
    }

    private String messageId(Message message, String body) throws Exception {
        String[] ids = message.getHeader("Message-ID");
        if (ids != null && ids.length > 0 && !ids[0].isBlank()) return ids[0].trim();
        String value = address(message.getFrom()) + '|' + message.getSubject() + '|'
                + message.getSentDate() + '|' + body;
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String address(Address[] addresses) {
        if (addresses == null || addresses.length == 0) return "";
        if (addresses[0] instanceof InternetAddress internet) return internet.getAddress();
        return addresses[0].toString();
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) throw new IllegalStateException("Email reconciliation is disabled");
        if (properties.getUsername() == null || properties.getUsername().isBlank())
            throw new IllegalStateException("GMAIL_IMAP_USERNAME is required");
        if (properties.getAppPassword() == null || properties.getAppPassword().isBlank())
            throw new IllegalStateException("GMAIL_IMAP_APP_PASSWORD is required");
    }
}
