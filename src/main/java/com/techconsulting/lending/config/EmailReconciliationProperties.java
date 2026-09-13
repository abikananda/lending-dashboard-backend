package com.techconsulting.lending.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "email-reconciliation")
public class EmailReconciliationProperties {
    private boolean enabled;
    private String host = "imap.gmail.com";
    private int port = 993;
    private String username;
    private String appPassword;
    private String folder = "INBOX";
    private int lookbackDays = 10;
    private String lendenclubSender = "noreply@lendenclub.com";
    private List<String> bankSenders = List.of("noreply@jana.bank.in", "noreply@slice.bank.in");

    public boolean isBankSender(String sender) {
        return sender != null && bankSenders.stream().anyMatch(sender::equalsIgnoreCase);
    }
}
