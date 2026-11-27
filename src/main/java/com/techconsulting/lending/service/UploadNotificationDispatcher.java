package com.techconsulting.lending.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class UploadNotificationDispatcher {
    private final ManualUploadEmailService emails;

    public void afterCommit(ManualUploadNotification event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { emails.send(event); }
            });
        } else {
            emails.send(event);
        }
    }
}
