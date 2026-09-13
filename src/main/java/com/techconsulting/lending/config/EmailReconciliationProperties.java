package com.techconsulting.lending.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
    private String bankSender = "noreply@jana.bank.in";
}
