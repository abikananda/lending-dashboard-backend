package com.techconsulting.lending.service;

import com.techconsulting.lending.config.EmailReconciliationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailReconciliationScheduler {
    private static final Logger log = LoggerFactory.getLogger(EmailReconciliationScheduler.class);
    private final EmailReconciliationProperties properties;
    private final EmailReconciliationService service;

    public EmailReconciliationScheduler(EmailReconciliationProperties properties,
                                        EmailReconciliationService service) {
        this.properties = properties; this.service = service;
    }

    @Scheduled(cron = "${email-reconciliation.cron:0 0 23 * * *}",
            zone = "${email-reconciliation.zone:Asia/Kolkata}")
    public void reconcileAtElevenPm() {
        if (!properties.isEnabled()) return;
        try {
            var result = service.syncAllEnabled();
            log.info("Scheduled email reconciliation completed imported={} duplicates={} parseFailures={} reconciliations={}",
                    result.importedEmails(), result.duplicateEmails(), result.parsingFailures(),
                    result.reconciliationsUpdated());
        } catch (RuntimeException ex) {
            log.error("Scheduled email reconciliation failed", ex);
        }
    }
}
